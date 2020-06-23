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

package uk.gov.hmrc.bankaccountreputationthirdpartycache.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.bankaccountreputationthirdpartycache.cache.ConfirmationOfPayeeCacheRepository
import uk.gov.hmrc.bankaccountreputationthirdpartycache.config.AppConfig
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton()
class CacheController @Inject()(appConfig: AppConfig, cc: ControllerComponents, repository: ConfirmationOfPayeeCacheRepository)
    extends BackendController(cc) {

  def cacheConfirmationOfPayee(): Action[JsValue] = Action.async(parse.json) { implicit request: Request[JsValue] =>
    import CacheRequest._

    Json.fromJson[CacheRequest](request.body).asOpt match {
      case Some(request) =>
        repository.insert(request.encryptedKey, request.encryptedData).map {
          case writeResult if writeResult.ok ⇒ NoContent
          case _ ⇒ InternalServerError("Could not cache the data")
        }
      case None ⇒
        Future.successful(BadRequest("Cache request was not valid"))
      }
  }

  def retrieveConfirmationOfPayee(): Action[JsValue] = Action.async(parse.json) { implicit request: Request[JsValue] =>
    import RetrieveRequest._

    Json.fromJson[RetrieveRequest](request.body).asOpt match {
      case Some(request) =>
        repository.findByRequest(request.encryptedKey).map {
          case Some(encryptedData) ⇒ Ok(encryptedData)
          case _ ⇒ InternalServerError("Could not retrieve cached data")
        }
      case _ ⇒
        Future.successful(BadRequest("Retrieve request was not valid"))
    }
  }
}

case class CacheRequest(encryptedKey: String, encryptedData: String)
object CacheRequest {
  implicit val cacheRequestReads: Reads[CacheRequest] = Json.reads[CacheRequest]
}

case class RetrieveRequest(encryptedKey: String)
object RetrieveRequest {
  implicit val cacheRequestReads: Reads[RetrieveRequest] = Json.reads[RetrieveRequest]
}
