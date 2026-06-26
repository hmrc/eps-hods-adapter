/*
 * Copyright 2021 HM Revenue & Customs
 *
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

    "return an Accepted response for Put request" in {

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

    "return an BAD_REQUEST status response for Empty Put request" in {

      val requestData = "identifier: nino"

      val request =
        FakeRequest(PUT, alertUrl).withBody(Json.toJson(requestData))

      val result = route(app, request)

      result.map(status) mustBe Some(BAD_REQUEST)

    }

    "return an Accepted response for Post request" in {

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
