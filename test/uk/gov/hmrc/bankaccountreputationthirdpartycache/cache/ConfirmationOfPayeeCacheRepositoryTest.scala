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

package uk.gov.hmrc.bankaccountreputationthirdpartycache.cache

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class ConfirmationOfPayeeCacheRepositoryTest extends WordSpec
  with Matchers
  with MongoSpecSupport
  with ScalaFutures
  with BeforeAndAfterEach
  with GuiceOneAppPerSuite
  with Eventually {

  override implicit val patienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  implicit override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(Map(
        "mongodb.uri" -> "mongodb://localhost:27017/test-bank-account-reputation-third-party-cache",
        "metrics.jvm" -> false)
      )
      .build()

  val mongoRepo: ConfirmationOfPayeeCacheRepository = app.injector.instanceOf(classOf[ConfirmationOfPayeeCacheRepository])

  override def beforeEach(): Unit = {
    super.beforeEach()
    mongoRepo.removeAll().futureValue
  }

  "Caching Repository" should {

    "create cache entry when asked and find by request" in {
      val request = Random.nextString(10)
      val response = "true, None, None"

      mongoRepo.insert(request, response).futureValue

      val cachedValue = mongoRepo.findByRequest(request).futureValue
      cachedValue shouldBe Some(response)
    }

    "create cache entry when asked and expire after TTL" in {
      val request = Random.nextString(10)
      val response = "true, None, None"

      mongoRepo.insert(request, response, 0).futureValue

      val cachedValue = mongoRepo.findByRequest(request).futureValue
      cachedValue shouldBe Some(response)

      eventually(timeout(60.seconds), interval(1.seconds)) {
        val cachedValue = mongoRepo.findByRequest(request).futureValue
        cachedValue shouldBe None
      }
    }
  }
}