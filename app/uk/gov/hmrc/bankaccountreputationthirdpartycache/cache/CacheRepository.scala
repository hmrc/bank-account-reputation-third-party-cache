/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, ReplaceOptions}
import org.mongodb.scala.result.UpdateResult
import uk.gov.hmrc.bankaccountreputationthirdpartycache.config.AppConfig
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
abstract class CacheRepository @Inject()(
                                          component: MongoComponent,
                                          collectionName: String,
                                          val expiryDays: Long = 0
                                        )(implicit ec: ExecutionContext, appConfig: AppConfig)
  extends PlayMongoRepository[EncryptedCacheEntry](component, collectionName, EncryptedCacheEntry.cacheFormat, Seq(
    IndexModel(ascending("key"), IndexOptions().name("uniqueKeyIndex").unique(true)),
    IndexModel(ascending("expiryDate"), IndexOptions().name("expiryDateIndex").expireAfter(expiryDays, TimeUnit.DAYS))
  ), replaceIndexes = appConfig.cacheReplaceIndexes()) {

  def findByRequest(encryptedKey: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    collection.find(equal("key", encryptedKey)).toFuture()
      .map(_.headOption)
      .map {
        case Some(ece) =>
          Some(ece.data)
        case None => None
      }
  }

  def store(encryptedKey: String, encryptedData: String): Future[UpdateResult] =
    collection.replaceOne(
      Filters.eq("key", encryptedKey),
      EncryptedCacheEntry(
        encryptedKey,
        encryptedData,
        Instant.now(Clock.systemUTC())),
      ReplaceOptions().upsert(true)
    ).toFuture()
}
