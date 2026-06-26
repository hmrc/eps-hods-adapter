/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, equalTo, get, post, postRequestedFor, urlEqualTo }
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{ JsObject, Json }
import play.api.test.FakeRequest
import uk.gov.hmrc.hods.model.nps.NpsPrintSuppressionUpdateRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.hods.model.nps.NpsPrintSuppressionUpdateRequest.OutputPreference
import utils.IntegrationSpec

import java.util.Base64

class NpsControllerHipISpec extends IntegrationSpec {
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                 -> server.port(),
        "microservice.services.citizen-details.port"      -> server.port(),
        "microservice.services.nps-hod-hip.host"          -> "127.0.0.1",
        "microservice.services.nps-hod-hip.port"          -> server.port(),
        "microservice.services.nps-hod-hip.originator-id" -> "MDTP-DC-TEST",
        "microservice.services.nps-hod-hip.client-id"     -> "id-local",
        "microservice.services.nps-hod-hip.client-secret" -> "secret-local",
        "microservice.services.preferences.port"          -> server.port()
      )
      .build()

  "/person/nino/print-suppression HIP API" must {
    val printSuppressionUrl = s"/eps-hods-adapter/preferences/person/$generatedNino/print-suppression"
    val etagUrl = s"/citizen-details/$generatedNino/etag"
    val hipPrefUrl = "/paye/individual/print-preferences"

    "return an OK response" in {
      val postRequest = Json.toJson(
        NpsPrintSuppressionUpdateRequest("NOT_KNOWN", OutputPreference.digital, false)
      )

      server.stubFor(
        get(urlEqualTo(etagUrl))
          .willReturn(aResponse().withStatus(OK).withBody("""{"etag" : "1"}"""))
      )

      server.stubFor(
        post(urlEqualTo(hipPrefUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(postRequest.toString)
          )
      )

      val request = FakeRequest(POST, printSuppressionUrl).withBody(postRequest)
      val result = route(app, request).get.futureValue
      result.header.status mustBe OK

      val token = Base64.getEncoder.encodeToString("id-local:secret-local".getBytes("UTF-8"))

      server.verify(
        postRequestedFor(urlEqualTo(hipPrefUrl))
          .withHeader(AUTHORIZATION, equalTo(s"Basic $token"))
          .withHeader(CONTENT_TYPE, equalTo(JSON))
          .withHeader("Gov-Uk-Originator-Id", equalTo("MDTP-DC-TEST"))
      );
    }

    "return an NOT_FOUND status response for NOT_FOUND response" in {

      val postRequest = Json.toJson(NpsPrintSuppressionUpdateRequest("formType", "outputPreference", false))

      server.stubFor(
        get(urlEqualTo(etagUrl))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      val request = FakeRequest(POST, printSuppressionUrl).withBody(postRequest)

      val result = route(app, request)

      result.map(status) mustBe Some(NOT_FOUND)
      result.map(contentAsString).map { message =>
        message must include("Got NOT_FOUND Status for nino")
      }
    }

    "return an UNPROCESSABLE_ENTITY status response for LOCKED response" in {
      val postRequest = Json.toJson(NpsPrintSuppressionUpdateRequest("formType", "outputPreference", false))

      server.stubFor(
        get(urlEqualTo(etagUrl))
          .willReturn(aResponse().withStatus(LOCKED))
      )

      val request = FakeRequest(POST, printSuppressionUrl).withBody(postRequest)
      val result = route(app, request)

      result.map(status) mustBe Some(UNPROCESSABLE_ENTITY)
      result.map(contentAsString).map { message =>
        message must include("This account is locked due to MCI")
      }
    }

    "return an SERVICE_UNAVAILABLE status response for SERVICE_UNAVAILABLE response" in {

      val postRequest = Json.toJson(NpsPrintSuppressionUpdateRequest("formType", "outputPreference", false))

      server.stubFor(
        get(urlEqualTo(etagUrl))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody("""{"etag" : "etag"}""")
          )
      )

      val request = FakeRequest(POST, printSuppressionUrl).withBody(postRequest)
      val result = route(app, request)
      result.map(status) mustBe Some(BAD_GATEWAY)
    }

    "return an BAD_GATEWAY status response for BAD_GATEWAY response" in {
      val postRequest = Json.toJson(NpsPrintSuppressionUpdateRequest("formType", "outputPreference", false))

      server.stubFor(
        get(urlEqualTo(etagUrl))
          .willReturn(
            aResponse()
              .withStatus(BAD_GATEWAY)
              .withBody("""{"etag" : "etag"}""")
          )
      )

      val request = FakeRequest(POST, printSuppressionUrl).withBody(postRequest)
      val result = route(app, request)
      result.map(status) mustBe Some(BAD_GATEWAY)
    }

    "return an BAD_REQUEST status response for BAD_REQUEST response" in {
      val postRequest = Json.toJson(NpsPrintSuppressionUpdateRequest("formType", "outputPreference", false))

      server.stubFor(
        get(urlEqualTo(etagUrl))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody("""{"etag" : "etag"}""")
          )
      )

      val request = FakeRequest(POST, printSuppressionUrl).withBody(postRequest)
      val result = route(app, request)
      result.map(status) mustBe Some(BAD_REQUEST)
    }

    "return an CONFLICT status response for CONFLICT HIP response" in {
      val postRequest = Json.toJson(NpsPrintSuppressionUpdateRequest("formType", "digital", false))

      server.stubFor(
        get(urlEqualTo(etagUrl))
          .willReturn(aResponse().withStatus(OK).withBody("""{"etag" : "1"}"""))
      )

      server.stubFor(
        post(urlEqualTo(hipPrefUrl))
          .willReturn(
            aResponse()
              .withStatus(CONFLICT)
              .withBody(Json.obj("failures" -> Json.obj("reason" -> "Its a conflict", "code" -> "1")).toString)
          )
      )

      val request = FakeRequest(POST, printSuppressionUrl).withBody(postRequest)
      val result = route(app, request)
      result.map(status) mustBe Some(CONFLICT)
      result.map(contentAsString).map { message =>
        message must include("Its a conflict")
      }
    }

    "return an UnprocessableEntity status response for UNPROCESSABLE_ENTITY des response" in {
      val postRequest = Json.toJson(NpsPrintSuppressionUpdateRequest("formType", "digital", false))

      server.stubFor(
        get(urlEqualTo(etagUrl))
          .willReturn(aResponse().withStatus(OK).withBody("""{"etag" : "0"}"""))
      )

      server.stubFor(
        post(urlEqualTo(hipPrefUrl))
          .willReturn(
            aResponse()
              .withStatus(UNPROCESSABLE_ENTITY)
              .withBody(NpsPrintSuppressionUpdateRequest("formType", "outputPreference", false).toString)
          )
      )

      val request = FakeRequest(POST, printSuppressionUrl).withBody(postRequest)
      val result = route(app, request)
      result.map(status) mustBe Some(UNPROCESSABLE_ENTITY)
    }
  }
}
