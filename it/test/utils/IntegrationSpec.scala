/*
 * Copyright 2026 HM Revenue & Customs
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

package utils

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, get, notFound, post, urlEqualTo }
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.Messages
import play.api.test.Injecting
import uk.gov.hmrc.domain.{ AtedUtr, AtedUtrGenerator, Nino, NinoGenerator }

import scala.concurrent.ExecutionContext

class IntegrationSpec
    extends AnyWordSpec with GuiceOneAppPerTest with Matchers with WireMockHelper with ScalaFutures
    with IntegrationPatience with Injecting {

  val generatedNino: Nino = NinoGenerator().nextNino

  val generatedSaUtr: AtedUtr = AtedUtrGenerator().nextAtedUtr

  lazy val messages: Messages = inject[Messages]

  implicit val ec: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  override def beforeEach(): Unit = {

    super.beforeEach()

    val authResponse =
      s"""
         |{
         |    "confidenceLevel": 200,
         |    "nino": "$generatedNino",
         |    "saUtr": "$generatedSaUtr",
         |    "name": {
         |        "name": "John",
         |        "lastName": "Smith"
         |    },
         |    "loginTimes": {
         |        "currentLogin": "2021-06-07T10:52:02.594Z",
         |        "previousLogin": null
         |    },
         |    "optionalCredentials": {
         |        "providerId": "4911434741952698",
         |        "providerType": "GovernmentGateway"
         |    },
         |    "authProviderId": {
         |        "ggCredId": "xyz"
         |    },
         |    "externalId": "testExternalId"
         |}
         |""".stripMargin

    server.stubFor(
      get(urlEqualTo("/")).willReturn(notFound())
    )

    server.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(aResponse().withBody(authResponse))
    )
  }

}
