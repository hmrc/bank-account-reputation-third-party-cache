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

import org.bson.types.ObjectId
import play.api.libs.json._

import java.time.Instant

case class EncryptedCacheEntry(key: String,
                               data: String,
                               expiryDate: Instant)

object EncryptedCacheEntry {

  import uk.gov.hmrc.mongo.play.json.formats.MongoFormats

  implicit val format: Format[ObjectId] = MongoFormats.objectIdFormat
  implicit val datetimeFormat: Format[Instant] =
    uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat

  implicit val cacheFormat: OFormat[EncryptedCacheEntry] = Json.format[EncryptedCacheEntry]
}
