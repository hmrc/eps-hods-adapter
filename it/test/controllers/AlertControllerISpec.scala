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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{ status as _, * }
import org.mongodb.scala.bson.ObjectId
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.hods.model.nps.*
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import utils.IntegrationSpec
import NoticeType.{ CY, CY_PLUS_1 }

import java.time.{ LocalDateTime, ZoneOffset }

class AlertControllerISpec extends IntegrationSpec {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"            -> server.port(),
        "microservice.services.citizen-details.port" -> server.port(),
        "microservice.services.nps-hod-des.host"     -> "127.0.0.1",
        "microservice.services.nps-hod-des.port"     -> server.port(),
        "microservice.services.preferences.port"     -> server.port()
      )
      .build()

  val alertUrl = "/eps-hods-adapter/preferences/alert"

  val basicPersonUpdatedUrl = s"/citizen-details/(.*)/designatory-details/basic"

  "/alert" must {

    "return an Accepted response for PUT request" when {
      "notice_type and parameters are not present in NpsAlert" in {

        server.stubFor(
          get(urlMatching(basicPersonUpdatedUrl))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(s"""{"nino" : "$generatedNino"}""")
            )
        )

        val putRequest = Json.toJson(Alert(NpsAlert(Identifier("nino", generatedNino.withoutSuffix), "nps", "0004")))

        val request = FakeRequest(PUT, alertUrl).withBody(putRequest)

        val result = route(app, request)

        result.map(status) mustBe Some(ACCEPTED)
      }

      "notice_type and parameters are present in NpsAlert" in {

        server.stubFor(
          get(urlMatching(basicPersonUpdatedUrl))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(s"""{"nino" : "$generatedNino"}""")
            )
        )

        val putRequest = Json.toJson(
          Alert(
            NpsAlert(
              identifier = Identifier("nino", generatedNino.withoutSuffix),
              hod_id = "nps",
              template_id = "0004",
              notice_type = Some(CY),
              parameters = Some(AlertParameter("2026"))
            )
          )
        )

        val request = FakeRequest(PUT, alertUrl).withBody(putRequest)

        val result = route(app, request)

        result.map(status) mustBe Some(ACCEPTED)
      }

      "notice_type is CY and parameters is not present in NpsAlert" in {

        server.stubFor(
          get(urlMatching(basicPersonUpdatedUrl))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(s"""{"nino" : "$generatedNino"}""")
            )
        )

        val putRequest = Json.toJson(
          Alert(
            NpsAlert(
              identifier = Identifier("nino", generatedNino.withoutSuffix),
              hod_id = "nps",
              template_id = "0004",
              notice_type = Some(CY)
            )
          )
        )

        val request = FakeRequest(PUT, alertUrl).withBody(putRequest)

        val result = route(app, request)

        result.map(status) mustBe Some(ACCEPTED)
      }
    }

    "return UNPROCESSABLE_ENTITY" when {

      "notice_type has a value of cy_plus_1 but taxYear parameter is not present in the payload" in {
        val putRequest = Json.toJson(
          Alert(NpsAlert(Identifier("nino", generatedNino.withoutSuffix), "nps", "0004", notice_type = Some(CY_PLUS_1)))
        )

        val request = FakeRequest(PUT, alertUrl).withBody(putRequest)

        val result = route(app, request)

        result.map(status) mustBe Some(UNPROCESSABLE_ENTITY)
      }
    }

    "return an BAD_REQUEST status response for Empty Put request" in {

      val requestData = "identifier: nino"

      val request =
        FakeRequest(PUT, alertUrl).withBody(Json.toJson(requestData))

      val result = route(app, request)

      result.map(status) mustBe Some(BAD_REQUEST)

    }

    "return an Accepted response for POST request" in {

      server.stubFor(
        get(urlMatching(basicPersonUpdatedUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(s"""{"nino" : "$generatedNino"}""")
          )
      )

      val putRequest = Json.toJson(Alert(NpsAlert(Identifier("nino", generatedNino.withoutSuffix), "nps", "0004")))

      val request = FakeRequest(POST, alertUrl).withBody(putRequest)

      val result = route(app, request)

      result.map(status) mustBe Some(ACCEPTED)
    }

    "return an BAD_REQUEST status response for Empty Post request" in {

      val requestData = "identifier: nino"

      val request =
        FakeRequest(POST, alertUrl).withBody(Json.toJson(requestData))

      val result = route(app, request)

      result.map(status) mustBe Some(BAD_REQUEST)

    }
  }

  "/alert/print-suppression/:id/status" must {

    val id: String = new ObjectId().toString
    val statusUrl =
      s"/eps-hods-adapter/preferences/alert/print-suppression/$id/status"
    val processStatus = ToDo
    val availableAt =
      LocalDateTime.of(2015, 8, 10, 13, 7, 2).toInstant(ZoneOffset.UTC)
    val changeStatus = ChangeStatus(processStatus, Some(availableAt))
    val requestData = Json.toJson(changeStatus)

    "return a BAD_REQUEST response for incorrect Post request" in {

      val request = FakeRequest(POST, "/eps-hods-adapter/preferences/alert/print-suppression/(.*)/status")
        .withBody(requestData)

      val result = route(app, request)

      result.map(status) mustBe Some(BAD_REQUEST)
    }

    "return a FORBIDDEN response for Post request" in {

      val request = FakeRequest(POST, statusUrl).withJsonBody(requestData)

      val result = route(app, request)

      result.map(status) mustBe Some(FORBIDDEN)
    }

    "return a NOT_FOUND response for GET request" in {

      val request = FakeRequest(GET, statusUrl)

      val result = route(app, request)

      result.map(status) mustBe Some(NOT_FOUND)
    }

  }

}
