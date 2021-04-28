/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.util.concurrent.TimeUnit

import javax.inject.Inject
import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.result.InsertOneResult
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.{ExecutionContext, Future}

abstract class CacheRepository @Inject()(component: MongoComponent, collectionName: String, val expiryDays: Long = 0)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[EncryptedCacheEntry](component, collectionName, EncryptedCacheEntry.mongoCacheFormat, Seq(
    IndexModel(ascending("key"), IndexOptions().name("uniqueKeyIndex").unique(true)),
    IndexModel(ascending("expiryDate"), IndexOptions().name("expiryDateIndex").expireAfter(expiryDays, TimeUnit.DAYS))
  ), replaceIndexes = true) {

  def findByRequest(encryptedKey: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    collection.find(equal("key", encryptedKey)).toFuture()
      .map(_.headOption)
      .map {
        case Some(ece) ⇒ Some(ece.data)
        case None ⇒ None
      }
  }

  def insert(encryptedKey: String, encryptedData: String)(implicit ec: ExecutionContext): Future[InsertOneResult] =
    collection.insertOne(
      EncryptedCacheEntry(ObjectId.get(),
        encryptedKey,
        encryptedData,
        LocalDateTime.now(ZoneOffset.UTC).plusDays(expiryDays))
    ).toFuture()
}
