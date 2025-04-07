/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubBodyParser

import java.util.Base64
import scala.concurrent.ExecutionContext

class BasicAuthActionSpec extends AnyWordSpec with MockitoSugar with Matchers {

  "allow request with valid credentials" in {
    val action = new BasicAuthAction("validUser", "validPass")(stubBodyParser(AnyContentAsEmpty))(ExecutionContext.global)
    val request = FakeRequest().withHeaders("Authorization" -> s"Basic ${Base64.getEncoder.encodeToString("validUser:validPass".getBytes)}")

    val result = action.filter(request).futureValue
    result shouldBe None
  }

  "deny request with invalid credentials" in {
    val action = new BasicAuthAction("validUser", "validPass")(stubBodyParser(AnyContentAsEmpty))(ExecutionContext.global)
    val request = FakeRequest().withHeaders("Authorization" -> s"Basic ${Base64.getEncoder.encodeToString("invalidUser:invalidPass".getBytes)}")

    val result = action.filter(request).futureValue
    result shouldBe Some(Results.Unauthorized)
  }

  "deny request with missing authorization header" in {
    val action = new BasicAuthAction("validUser", "validPass")(stubBodyParser(AnyContentAsEmpty))(ExecutionContext.global)
    val request = FakeRequest()

    val result = action.filter(request).futureValue
    result shouldBe Some(Results.Unauthorized)
  }
}
