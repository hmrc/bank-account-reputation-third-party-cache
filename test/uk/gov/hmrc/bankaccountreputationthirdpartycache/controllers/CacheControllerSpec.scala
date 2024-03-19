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
import play.api.libs.json.Json
import play.api.mvc.{Headers, Result}
import play.api.test.{FakeRequest, Helpers}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.bankaccountreputationthirdpartycache.cache.{CallValidateCacheRepository, ConfirmationOfPayeeBusinessCacheRepository, ConfirmationOfPayeePersonalCacheRepository}
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
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp, cv)

      when(cpp.findByRequest(any())(any())).thenReturn(Future.successful(Some("some_data")))

      assertRetrieveResult(controller.retrieveConfirmationOfPayeePersonal()(fakeRetrieveRequest))
    }

    "return NotFound(404) when key not found in cache" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp, cv)

      when(cpp.findByRequest(any())(any())).thenReturn(Future.successful(None))

      assertRetrieve404(controller.retrieveConfirmationOfPayeePersonal()(fakeRetrieveRequest))
    }
  }

  "POST /confirmation-of-payee/personal/store" should {
    "return Ok(200) and cache the key and data" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp, cv)

      val mockWriteResult: UpdateResult = mock[UpdateResult]
      when(mockWriteResult.wasAcknowledged()).thenReturn(true)
      when(cpp.store(any(), any())(any())).thenReturn(Future.successful(mockWriteResult))

      assertStoreResult(controller.storeConfirmationOfPayeePersonal()(fakeStoreRequest))
    }
  }

  "POST /confirmation-of-payee/business/retrieve" should {
    "return Ok(200) and the value" in new Setup() {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp, cv)

      when(cpb.findByRequest(any())(any())).thenReturn(Future.successful(Some("some_data")))

      assertRetrieveResult(controller.retrieveConfirmationOfPayeeBusiness()(fakeRetrieveRequest))
    }

    "return NotFound(404) when key not found in cache" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp, cv)

      when(cpb.findByRequest(any())(any())).thenReturn(Future.successful(None))

      assertRetrieve404(controller.retrieveConfirmationOfPayeeBusiness()(fakeRetrieveRequest))
    }
  }

  "POST /confirmation-of-payee/business/store" should {
    "return Ok(200) and cache the key and data" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp, cv)

      val mockWriteResult: UpdateResult = mock[UpdateResult]
      when(mockWriteResult.wasAcknowledged()).thenReturn(true)
      when(cpb.store(any(), any())(any())).thenReturn(Future.successful(mockWriteResult))

      assertStoreResult(controller.storeConfirmationOfPayeeBusiness()(fakeStoreRequest))
    }
  }

  "POST /call-validate/retrieve" should {
    "return Ok(200) and the value" in new Setup() {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp, cv)

      when(cv.findByRequest(any())(any())).thenReturn(Future.successful(Some("some_data")))

      assertRetrieveResult(controller.retrieveCallValidate()(fakeRetrieveRequest))
    }

    "return NotFound(404) when key not found in cache" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp, cv)

      when(cv.findByRequest(any())(any())).thenReturn(Future.successful(None))

      assertRetrieve404(controller.retrieveCallValidate()(fakeRetrieveRequest))
    }
  }

  "POST /call-validate/store" should {
    "return Ok(200) and cache the key and data" in new Setup {
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), cpb, cpp, cv)

      val mockWriteResult: UpdateResult = mock[UpdateResult]
      when(mockWriteResult.wasAcknowledged()).thenReturn(true)
      when(cv.store(any(), any())(any())).thenReturn(Future.successful(mockWriteResult))

      assertStoreResult(controller.storeCallValidate()(fakeStoreRequest))
    }
  }

  trait Setup {
    val cpb: ConfirmationOfPayeeBusinessCacheRepository = mock[ConfirmationOfPayeeBusinessCacheRepository]
    val cpp: ConfirmationOfPayeePersonalCacheRepository = mock[ConfirmationOfPayeePersonalCacheRepository]
    val cv: CallValidateCacheRepository = mock[CallValidateCacheRepository]

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
