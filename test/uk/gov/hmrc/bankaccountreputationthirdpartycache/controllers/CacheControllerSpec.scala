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

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.MediaTypes
import org.apache.pekko.stream.{ActorMaterializer, Materializer}
import org.apache.pekko.util.Timeout
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers._
import org.mongodb.scala.result.UpdateResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Headers, Result}
import play.api.test.{FakeRequest, Helpers}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.bankaccountreputationthirdpartycache.cache.{ConfirmationOfPayeeBusinessCacheRepository, ConfirmationOfPayeePersonalCacheRepository}
import uk.gov.hmrc.bankaccountreputationthirdpartycache.config.AppConfig
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CacheControllerSpec extends AnyWordSpec with MockitoSugar with Matchers {

  import scala.concurrent.duration._

  implicit val system: ActorSystem = ActorSystem("CacheControllerSpec")

  implicit def mat: Materializer = ActorMaterializer()

  implicit val timeout: Timeout = Timeout(5.seconds)

  val auth: String = Base64.getEncoder.encodeToString(s"bars:token".getBytes(StandardCharsets.UTF_8))

  private val fakeRetrieveRequest = FakeRequest("POST", "/retrieve",
    headers = Headers(("Content-Type", "application/json"), HeaderNames.authorisation -> s"Basic $auth"),
    body = Json.parse("""{"encryptedKey": "blah"}"""))

  private val fakeStoreRequest = FakeRequest("POST", "/store",
    headers = Headers(("Content-Type", "application/json"), HeaderNames.authorisation -> s"Basic $auth"),
    body = Json.parse("""{"encryptedKey": "blah","encryptedData": "blah blah"}"""))

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig = new AppConfig(configuration, serviceConfig)

  "POST /confirmation-of-payee/personal/retrieve" should {
    "return Ok(200) and the value" in new Setup() {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      when(cpp.findByRequest(any())(any())).thenReturn(Future.successful(Some("some_data")))

      assertRetrieveResult(controller.retrieveConfirmationOfPayeePersonal()(fakeRetrieveRequest))
    }

    "return NotFound(404) when key not found in cache" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      when(cpp.findByRequest(any())(any())).thenReturn(Future.successful(None))

      assertRetrieve404(controller.retrieveConfirmationOfPayeePersonal()(fakeRetrieveRequest))
    }

    "return InternalServerError(500) when an error occurs" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      when(cpp.findByRequest(any())(any())).thenReturn(Future.failed(new Exception("error")))

      val result: Future[Result] = controller.retrieveConfirmationOfPayeePersonal()(fakeRetrieveRequest)
      Helpers.status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return BadRequest(400) when the request is invalid" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      val invalidRequest: FakeRequest[JsValue] = fakeRetrieveRequest.withBody(Json.parse("""{"invalidKey": "blah"}"""))

      val result: Future[Result] = controller.retrieveConfirmationOfPayeePersonal()(invalidRequest)
      Helpers.status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "POST /confirmation-of-payee/personal/store" should {
    "return Ok(200) and cache the key and data" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      val mockWriteResult: UpdateResult = mock[UpdateResult]
      when(mockWriteResult.wasAcknowledged()).thenReturn(true)
      when(cpp.store(any(), any())(any())).thenReturn(Future.successful(mockWriteResult))

      assertStoreResult(controller.storeConfirmationOfPayeePersonal()(fakeStoreRequest))
    }

    "return InternalServerError(500) when the record is not stored in the collection" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      val mockWriteResult: UpdateResult = mock[UpdateResult]
      when(mockWriteResult.wasAcknowledged()).thenReturn(false)
      when(cpp.store(any(), any())(any())).thenReturn(Future.successful(mockWriteResult))

      val result: Future[Result] = controller.storeConfirmationOfPayeePersonal()(fakeStoreRequest)
      Helpers.status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return InternalServerError(500) when an error occurs" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      when(cpp.store(any(), any())(any())).thenReturn(Future.failed(new Exception("error")))

      val result: Future[Result] = controller.storeConfirmationOfPayeePersonal()(fakeStoreRequest)
      Helpers.status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return BadRequest(400) when the request is invalid" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      val invalidRequest: FakeRequest[JsValue] = fakeStoreRequest.withBody(Json.parse("""{"invalidKey": "blah"}"""))

      val result: Future[Result] = controller.storeConfirmationOfPayeePersonal()(invalidRequest)
      Helpers.status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "POST /confirmation-of-payee/business/retrieve" should {
    "return Ok(200) and the value" in new Setup() {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      when(cpb.findByRequest(any())(any())).thenReturn(Future.successful(Some("some_data")))

      assertRetrieveResult(controller.retrieveConfirmationOfPayeeBusiness()(fakeRetrieveRequest))
    }

    "return NotFound(404) when key not found in cache" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      when(cpb.findByRequest(any())(any())).thenReturn(Future.successful(None))

      assertRetrieve404(controller.retrieveConfirmationOfPayeeBusiness()(fakeRetrieveRequest))
    }

    "return InternalServerError(500) when an error occurs" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      when(cpb.findByRequest(any())(any())).thenReturn(Future.failed(new Exception("error")))

      val result: Future[Result] = controller.retrieveConfirmationOfPayeeBusiness()(fakeRetrieveRequest)
      Helpers.status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return BadRequest(400) when the request is invalid" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      val invalidRequest: FakeRequest[JsValue] = fakeRetrieveRequest.withBody(Json.parse("""{"invalidKey": "blah"}"""))

      val result: Future[Result] = controller.retrieveConfirmationOfPayeeBusiness()(invalidRequest)
      Helpers.status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "POST /confirmation-of-payee/business/store" should {
    "return Ok(200) and cache the key and data" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      val mockWriteResult: UpdateResult = mock[UpdateResult]
      when(mockWriteResult.wasAcknowledged()).thenReturn(true)
      when(cpb.store(any(), any())(any())).thenReturn(Future.successful(mockWriteResult))

      assertStoreResult(controller.storeConfirmationOfPayeeBusiness()(fakeStoreRequest))
    }

    "return InternalServerError(500) when the record is not stored in the collection" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      val mockWriteResult: UpdateResult = mock[UpdateResult]
      when(mockWriteResult.wasAcknowledged()).thenReturn(false)
      when(cpb.store(any(), any())(any())).thenReturn(Future.successful(mockWriteResult))

      val result: Future[Result] = controller.storeConfirmationOfPayeeBusiness()(fakeStoreRequest)
      Helpers.status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return InternalServerError(500) when an error occurs" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      when(cpb.store(any(), any())(any())).thenReturn(Future.failed(new Exception("error")))

      val result: Future[Result] = controller.storeConfirmationOfPayeeBusiness()(fakeStoreRequest)
      Helpers.status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return BadRequest(400) when the request is invalid" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp)

      val invalidRequest: FakeRequest[JsValue] = fakeStoreRequest.withBody(Json.parse("""{"invalidKey": "blah"}"""))

      val result: Future[Result] = controller.storeConfirmationOfPayeeBusiness()(invalidRequest)
      Helpers.status(result) shouldBe Status.BAD_REQUEST
    }
  }
  
  trait Setup {
    val cpb: ConfirmationOfPayeeBusinessCacheRepository = mock[ConfirmationOfPayeeBusinessCacheRepository]
    val cpp: ConfirmationOfPayeePersonalCacheRepository = mock[ConfirmationOfPayeePersonalCacheRepository]

    def assertRetrieveResult(result: Future[Result]): Unit = {
      Helpers.status(result) shouldBe Status.OK
      Helpers.contentAsString(result) shouldBe """{"encryptedData":"some_data"}"""
      Helpers.contentType(result) shouldBe Some(MediaTypes.`application/json`.value)
    }

    def assertRetrieve404(result: Future[Result]): Unit = {
      Helpers.status(result) shouldBe Status.NOT_FOUND
      Helpers.contentType(result) shouldBe Some(MediaTypes.`application/json`.value)
    }

    def assertStoreResult(result: Future[Result]): Unit = {
      Helpers.status(result) shouldBe Status.OK
      Helpers.contentAsString(result) shouldBe """{"stored":true}"""
      Helpers.contentType(result) shouldBe Some(MediaTypes.`application/json`.value)
    }
  }

}
