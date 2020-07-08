/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bankaccountreputationthirdpartycache.cache

import java.time.{ZoneOffset, ZonedDateTime}

import javax.inject.Inject
import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONLong, BSONObjectID}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

abstract class CacheRepository @Inject()(component: ReactiveMongoComponent, collectionName: String)
  extends ReactiveRepository[EncryptedCacheEntry, BSONObjectID](collectionName, component.mongoConnector.db, EncryptedCacheEntry.mongoCacheFormat) {

  def expiryDays: Int

  val expireAfterSeconds: Long = 0

  private lazy val ExpiryDateIndex = "expiryDateIndex"
  private lazy val UniqueKeyIndex = "uniqueKeyIndex"
  private lazy val OptExpireAfterSeconds = "expireAfterSeconds"

  def findByRequest(encryptedKey: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    find("key" → encryptedKey)
      .map(_.headOption)
      .map {
        case Some(ece) ⇒ Some(ece.data)
        case None ⇒ None
      }
  }

  def insert(encryptedKey: String, encryptedData: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    collection.insert(
      EncryptedCacheEntry(BSONObjectID.generate(),
        encryptedKey,
        encryptedData,
        ZonedDateTime.now(ZoneOffset.UTC).plusDays(expiryDays))
    )

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    import reactivemongo.bson.DefaultBSONHandlers._

    val indexes = collection.indexesManager.list()
    indexes.flatMap { idxs =>
      val unique = idxs.find(index => index.eventualName == UniqueKeyIndex)
      val expiry = idxs.find(index =>
        index.eventualName == ExpiryDateIndex
          && index.options.getAs[BSONLong](OptExpireAfterSeconds).fold(false)(_.as[Long] != expireAfterSeconds))

      Future.sequence(Seq(ensureExpiryDateIndex(expiry), ensureUniqueKeyIndex(unique)))
    }
  }

  private def ensureExpiryDateIndex(existingIndex: Option[Index])(implicit ec: ExecutionContext) = {
    Logger.info(s"Creating time to live for entries in ${collection.name} to $expireAfterSeconds seconds")

    existingIndex.fold(Future.successful(0)) { idx => collection.indexesManager.drop(idx.eventualName) }
      .flatMap { _ =>
        collection.indexesManager.ensure(
          Index(
            key = Seq("expiryDate" -> IndexType.Ascending),
            name = Some(ExpiryDateIndex),
            options = BSONDocument(OptExpireAfterSeconds -> expireAfterSeconds)))
      }
  }

  private def ensureUniqueKeyIndex(existingIndex: Option[Index])(implicit ec: ExecutionContext) = {
    collection.indexesManager.ensure(
      Index(
        key = Seq("key" -> IndexType.Ascending),
        name = Some(UniqueKeyIndex),
        options = BSONDocument("unique" -> true)
      )
    )
  }
}
