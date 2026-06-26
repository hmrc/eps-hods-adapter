/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, post, serverError }
import com.github.tomakehurst.wiremock.http.Fault
import org.bson.types.ObjectId
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.hods.util.{ BaseSpec, WireMockHelper }
import uk.gov.hmrc.http.HttpResponse

class EpsMessageRendererConnectorSpec
    extends BaseSpec with WireMockHelper with PatienceConfiguration with MockitoSugar {

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.eps-message-renderer.port" -> server.port()
      )
    )
    .build()

  def sut = new EpsMessageRendererConnector(http, appConfig)

  "EpsMessageRendererConnector" when {

    "postNotification is called" should {

      val url = "/eps-message-renderer/process-notification"

      "respond with a success response" in {
        val id = ObjectId()
        val body: String =
          s"""
             |{"id":"$id",
             |"modifiedDetails":{"createdAt":"2015-05-01T13:07:02.000Z","lastUpdated":"2015-05-01T13:07:02.000Z"},
             |"availableAt":"2015-05-01T13:07:02.000Z",
             |"status":"todo",
             |"failures":1,
             |"alerts":{"alert":{"identifier":{"id_type":"nino","value":"AA000003"},"hod_id":"nps","template_id":"0004"}},
             |"statusUrl":"/eps-hods-adapter/preferences/alert/print-suppression/$id/status"
             |}""".stripMargin

        server.stubFor(
          post(url)
            .willReturn(aResponse().withStatus(OK))
        )

        val result: HttpResponse = await(sut.postNotification(Json.parse(body)))

        result.status mustBe OK
      }

      "respond with a failure response" in {
        server.stubFor(
          post(url)
            .willReturn(serverError())
        )

        val result: HttpResponse = await(sut.postNotification(Json.parse("{}")))

        result.status mustBe INTERNAL_SERVER_ERROR
      }

      "respond with a handled error response" in {
        server.stubFor(
          post(url)
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
        )

        val result: HttpResponse = await(sut.postNotification(Json.parse("{}")))

        result.status mustBe GATEWAY_TIMEOUT
      }
    }
  }
}
