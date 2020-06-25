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

import akka.http.scaladsl.model.MediaTypes
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.bankaccountreputationthirdpartycache.cache.ConfirmationOfPayeeCacheRepository
import uk.gov.hmrc.bankaccountreputationthirdpartycache.config.AppConfig
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton()
class CacheController @Inject()(appConfig: AppConfig, cc: ControllerComponents, repository: ConfirmationOfPayeeCacheRepository)
    extends BackendController(cc) {

  private val WithBasicAuth = new BasicAuthAction[JsValue]("bars", appConfig.basicAuthToken)(parse.json)

  def storeConfirmationOfPayee(): Action[JsValue] = WithBasicAuth.async { implicit request: Request[JsValue] =>
    import StoreRequest._
    import StoreResponse._

    Json.fromJson[StoreRequest](request.body).asOpt match {
      case Some(request) =>
        repository.insert(request.encryptedKey, request.encryptedData).map {
          case writeResult if writeResult.ok ⇒ Ok(Json.toJson(
            StoreResponse(stored = true, description =  None))
          ).as(MediaTypes.`application/json`.value)
          case _ ⇒ InternalServerError(Json.toJson(
            StoreResponse(stored = false, description =  Some("Could not cache the data")))
          ).as(MediaTypes.`application/json`.value)
        }
      case None ⇒
        Future.successful(BadRequest(Json.toJson(
          StoreResponse(stored = false, description =  Some("Cache request was not valid")))
        ).as(MediaTypes.`application/json`.value)
        )
      }
  }

  def retrieveConfirmationOfPayee(): Action[JsValue] = WithBasicAuth.async { implicit request: Request[JsValue] =>
    import RetrieveRequest._
    import RetrieveResponse._

    Json.fromJson[RetrieveRequest](request.body).asOpt match {
      case Some(request) =>
        repository.findByRequest(request.encryptedKey).map {
          case Some(encryptedData) ⇒
            Ok(Json.toJson(
              RetrieveResponse(encryptedData = Some(encryptedData), description = None))
            ).as(MediaTypes.`application/json`.value)
          case _ ⇒
            NotFound(Json.toJson(
              RetrieveResponse(encryptedData = None, description = Some("Could not find data for given key")))
            ).as(MediaTypes.`application/json`.value)
        }
      case _ ⇒
        Future.successful(
          BadRequest(Json.toJson(
            RetrieveResponse(encryptedData = None, description = Some("Retrieve request was not valid")))
          ).as(MediaTypes.`application/json`.value))
    }
  }
}

case class StoreRequest(encryptedKey: String, encryptedData: String)
object StoreRequest {
  implicit val storeRequestReads: Reads[StoreRequest] = Json.reads[StoreRequest]
}

case class StoreResponse(stored: Boolean, description: Option[String])
object StoreResponse {
  implicit val retrieveResponseWrites: Writes[StoreResponse] = Json.writes[StoreResponse]
}

case class RetrieveRequest(encryptedKey: String)
object RetrieveRequest {
  implicit val retrieveRequestReads: Reads[RetrieveRequest] = Json.reads[RetrieveRequest]
}

case class RetrieveResponse(encryptedData: Option[String], description: Option[String])
object RetrieveResponse {
  implicit val retrieveResponseWrites: Writes[RetrieveResponse] = Json.writes[RetrieveResponse]
}
