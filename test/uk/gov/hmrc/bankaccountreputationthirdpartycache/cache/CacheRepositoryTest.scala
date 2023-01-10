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

import org.mongodb.scala.bson.collection.immutable.Document
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class CacheRepositoryTest extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterEach
  with GuiceOneAppPerSuite
  with Eventually {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  implicit override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(Map(
        "mongodb.uri" -> "mongodb://localhost:27017/test-bank-account-reputation-third-party-cache",
        "metrics.jvm" -> false)
      )
      .build()

  val mongoComponent: MongoComponent = app.injector.instanceOf[MongoComponent]
  val mongoRepo: CacheRepository = new CacheRepository(mongoComponent, "test-cache", expiryDays = 0) {}

  override def beforeEach(): Unit = {
    super.beforeEach()
    mongoRepo.collection.deleteMany(Document()).toFuture().futureValue
  }

  "Caching Repository" should {

    "create cache entry when asked and find by request" in {
      val request = Random.nextString(10)
      val response = "true, None, None"

      mongoRepo.store(request, response).futureValue

      val cachedValue = mongoRepo.findByRequest(request).futureValue
      cachedValue shouldBe Some(response)
    }

    "update a cache entry if it already exists" in {
      val request = Random.nextString(10)
      val response = "true, None, None"
      val anotherResponse = "false, None, None"

      mongoRepo.store(request, response).futureValue

      val cachedValue = mongoRepo.findByRequest(request).futureValue
      cachedValue shouldBe Some(response)

      mongoRepo.store(request, anotherResponse).futureValue

      val updatedValue = mongoRepo.findByRequest(request).futureValue
      updatedValue shouldBe Some(anotherResponse)
    }

    "create cache entry when asked and expire after TTL" in {
      val request = Random.nextString(10)
      val response = "true, None, None"

      mongoRepo.store(request, response).futureValue

      val cachedValue = mongoRepo.findByRequest(request).futureValue
      cachedValue shouldBe Some(response)

      eventually(timeout(60.seconds), interval(1.seconds)) {
        val cachedValue = mongoRepo.findByRequest(request).futureValue
        cachedValue shouldBe None
      }
    }
  }
}