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

import uk.gov.hmrc.bankaccountreputationthirdpartycache.config.AppConfig
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmationOfPayeeBusinessCacheRepository @Inject()(component: MongoComponent)(implicit ec: ExecutionContext, appConfig: AppConfig)
  extends CacheRepository(component, "confirmation-of-payee-business-cache", appConfig.payeeBusinessCacheItemExpiryDays) {
}