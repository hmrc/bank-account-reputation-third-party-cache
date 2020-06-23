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

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Headers
import play.api.test.{FakeRequest, Helpers}
import play.api.{Configuration, Environment, Mode}
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.bankaccountreputationthirdpartycache.cache.ConfirmationOfPayeeCacheRepository
import uk.gov.hmrc.bankaccountreputationthirdpartycache.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

import scala.concurrent.Future

class CacheControllerSpec extends WordSpec with MockitoSugar with Matchers {

  import scala.concurrent.duration._

  implicit val system: ActorSystem = ActorSystem("CacheControllerSpec")

  implicit def mat: Materializer = ActorMaterializer()

  implicit val timeout: Timeout = Timeout(5.seconds)

  private val rootUri = "/bank-account-reputation-third-party-cache"
  private val fakeGetRequest = FakeRequest("GET", rootUri + "/confirmation-of-payee/", headers = Headers(("Content-Type","application/json")), body = Json.parse("""{"encryptedKey": "blah"}"""))

  private val fakePutRequest = FakeRequest("POST", rootUri + "/confirmation-of-payee/", headers = Headers(("Content-Type","application/json")), body = Json.parse("""{"encryptedKey": "blah","encryptedData": "blah blah"}"""))

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration, new RunMode(configuration, Mode.Dev))
  private val appConfig = new AppConfig(configuration, serviceConfig)


  "GET /" should {
    "return 200 and the value" in {
      val mockRepository = mock[ConfirmationOfPayeeCacheRepository]
      when(mockRepository.findByRequest(any())(any())).thenReturn(Future.successful(Some("some_data")))
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), mockRepository)
      val result = controller.retrieveConfirmationOfPayee()(fakeGetRequest)
      Helpers.status(result) shouldBe Status.OK
      Helpers.contentAsString(result) shouldBe "some_data"
    }
  }

  "POST cache" should {
    "return 200 and cache the key and data" in {
      val mockRepository = mock[ConfirmationOfPayeeCacheRepository]
      val mockWriteResult = mock[WriteResult]
      when(mockWriteResult.ok).thenReturn(true)
      when(mockRepository.insert(any(), any())(any())).thenReturn(Future.successful(mockWriteResult))
      val controller = new CacheController(appConfig, Helpers.stubControllerComponents(), mockRepository)
      val result = controller.cacheConfirmationOfPayee()(fakePutRequest)
      Helpers.status(result) shouldBe Status.NO_CONTENT
    }
  }
}
