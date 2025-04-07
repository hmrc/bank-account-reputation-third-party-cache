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

package uk.gov.hmrc.bankaccountreputationthirdpartycache.controllers

import org.apache.pekko.http.scaladsl.model.MediaTypes
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.bankaccountreputationthirdpartycache.cache.{CacheRepository, ConfirmationOfPayeeBusinessCacheRepository, ConfirmationOfPayeePersonalCacheRepository}
import uk.gov.hmrc.bankaccountreputationthirdpartycache.config.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class CacheController @Inject()(appConfig: AppConfig, cc: ControllerComponents,
                                confirmationOfPayeeBusinessCacheRepository:  ConfirmationOfPayeeBusinessCacheRepository,
                                confirmationOfPayeePersonalCacheRepository: ConfirmationOfPayeePersonalCacheRepository)(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  private def WithBasicAuth = new BasicAuthAction[JsValue]("bars", appConfig.basicAuthToken)(parse.json)

  val logger: Logger = Logger("CacheController")

  def storeConfirmationOfPayeeBusiness(): Action[JsValue] = WithBasicAuth.async { implicit request: Request[JsValue] =>
    store(request, confirmationOfPayeeBusinessCacheRepository)
  }

  def retrieveConfirmationOfPayeeBusiness(): Action[JsValue] = WithBasicAuth.async { implicit request: Request[JsValue] =>
    retrieve(request, confirmationOfPayeeBusinessCacheRepository)
  }

  def storeConfirmationOfPayeePersonal(): Action[JsValue] = WithBasicAuth.async { implicit request: Request[JsValue] =>
    store(request, confirmationOfPayeePersonalCacheRepository)
  }

  def retrieveConfirmationOfPayeePersonal(): Action[JsValue] = WithBasicAuth.async { implicit request: Request[JsValue] =>
    retrieve(request, confirmationOfPayeePersonalCacheRepository)
  }

  private def store(request: Request[JsValue], repository: CacheRepository) = {
    import StoreRequest._
    import StoreResponse._

    Json.fromJson[StoreRequest](request.body).asOpt match {
      case Some(request) =>
        repository.store(request.encryptedKey, request.encryptedData).map {
          case result if result.wasAcknowledged() => Ok(Json.toJson(
            StoreResponse(stored = true, description = None))
          ).as(MediaTypes.`application/json`.value)
          case _ =>
            logger.error(s"Could not cache the data")
            InternalServerError(Json.toJson(
            StoreResponse(stored = false, description = Some("Could not cache the data")))
          ).as(MediaTypes.`application/json`.value)
        }.recoverWith {
          case t =>
            logger.error(s"Error inserting cache data: ${t.getMessage}")
            Future.successful(InternalServerError(Json.toJson(
              StoreResponse(stored = false, description = Some("Error inserting cache data.")))
            ).as(MediaTypes.`application/json`.value))
        }
      case None =>
        Future.successful(BadRequest(Json.toJson(
          StoreResponse(stored = false, description = Some("Cache request was not valid")))
        ).as(MediaTypes.`application/json`.value)
        )
    }
  }

  private def retrieve(request: Request[JsValue], repository: CacheRepository) = {
    import RetrieveRequest._
    import RetrieveResponse._

    Json.fromJson[RetrieveRequest](request.body).asOpt match {
      case Some(request) =>
        repository.findByRequest(request.encryptedKey).map {
          case Some(encryptedData) =>
            Ok(Json.toJson(
              RetrieveResponse(encryptedData = Some(encryptedData), description = None))
            ).as(MediaTypes.`application/json`.value)
          case _ =>
            NotFound(Json.toJson(
              RetrieveResponse(encryptedData = None, description = Some("Could not find data for given key")))
            ).as(MediaTypes.`application/json`.value)
        }.recoverWith {
          case _ =>
            Future.successful(InternalServerError(Json.toJson(
              StoreResponse(stored = false, description = Some("Error retrieving cache data.")))
            ).as(MediaTypes.`application/json`.value))
        }
      case _ =>
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
