/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, get, ok, urlEqualTo }
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.IntegrationSpec

class NpsControllerISpec extends IntegrationSpec {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"            -> server.port(),
        "microservice.services.citizen-details.port" -> server.port(),
        "microservice.services.preferences.port"     -> server.port()
      )
      .build()

  "/person/nino" must {

    val url = s"/eps-hods-adapter/preferences/person/$generatedNino"

    val basicPersonUrl =
      s"/citizen-details/$generatedNino/designatory-details/basic"

    "return an OK response" in {

      server.stubFor(
        get(urlEqualTo(basicPersonUrl))
          .willReturn(
            ok("""[
                 |  {
                 |    "nino": "AB123456C"
                 |  }
                 |]""".stripMargin)
          )
      )

      val request = FakeRequest(GET, url)

      val result = route(app, request)

      result.map(status) mustBe Some(OK)
    }

    "return an BAD_GATEWAY status response for BAD_REQUEST response" in {

      server.stubFor(
        get(urlEqualTo(basicPersonUrl))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Bad Request"))
      )

      val request = FakeRequest(GET, url)

      val result = route(app, request)

      result.map(status) mustBe Some(BAD_GATEWAY)

      result.map(contentAsString).map { message =>
        message must include("Bad Request")
      }
    }

    "return an BAD_GATEWAY status response for SERVICE_UNAVAILABLE response" in {

      server.stubFor(
        get(urlEqualTo(basicPersonUrl))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
              .withBody("Service Unavailable")
          )
      )

      val request = FakeRequest(GET, url)

      val result = route(app, request)

      result.map(status) mustBe Some(BAD_GATEWAY)

      result.map(contentAsString).map { message =>
        message must include("Service Unavailable")
      }
    }

    "return an LOCKED status response for LOCKED response" in {

      server.stubFor(
        get(urlEqualTo(basicPersonUrl))
          .willReturn(aResponse().withStatus(LOCKED).withBody("locked"))
      )

      val request = FakeRequest(GET, url)

      val result = route(app, request)

      result.map(status) mustBe Some(LOCKED)

      result.map(contentAsString).map { message =>
        message must include("locked")
      }
    }

    "return an NOT_FOUND status response for NOT_FOUND response" in {

      server.stubFor(
        get(urlEqualTo(basicPersonUrl))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("not found"))
      )

      val request = FakeRequest(GET, url)

      val result = route(app, request)

      result.map(status) mustBe Some(NOT_FOUND)

      result.map(contentAsString).map { message =>
        message must include("not found")
      }
    }

  }
}
