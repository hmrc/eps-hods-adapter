/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, get }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import uk.gov.hmrc.hods.util.{ BaseSpec, WireMockHelper }
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.http.{ GatewayTimeoutException, HeaderCarrier, HttpResponse }
import scala.concurrent.Future

class CitizenDetailsConnectorSpec extends BaseSpec with WireMockHelper with PatienceConfiguration with MockitoSugar {

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.citizen-details.port" -> server.port()
      )
    )
    .build()

  def sut = new CitizenDetailsConnector(http, appConfig, auditLog, metrics)

  "CitizenDetailsConnector" when {

    "getPerson is called" should {

      val url: String =
        s"/citizen-details/${nino.nino}/designatory-details/basic"

      "respond with a valid person response" in {

        val body =
          s"""{"firstName": "firstname", "lastName": "lastname", "title":"Ms", "nino":"${nino.nino}"}"""

        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(OK).withBody(body))
        )

        val result = await(sut.getPersonDetails(nino.nino))

        result.status mustBe OK
        result.json.toString() `contains` body
      }

      "respond to a 400 by propagating BAD REQUEST" in {

        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        await(sut.getPersonDetails(nino.nino)).status mustBe BAD_REQUEST
      }

      "respond to a 400 by propagating NOT FOUND" in {

        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        await(sut.getPersonDetails(nino.nino)).status mustBe NOT_FOUND
      }

      "Respond to another 4xx error being reported by the httpGet instance by propagating the response code" in {

        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(LOCKED))
        )

        await(sut.getPersonDetails(nino.nino)).status mustBe LOCKED

      }

      "Respond to another 5xx error being reported by the httpGet instance by propagating the response code" in {

        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )

        await(sut.getPersonDetails(nino.nino)).status mustBe INTERNAL_SERVER_ERROR
      }
    }

    "getEtag is called" should {

      val url: String = s"/citizen-details/$nino/etag"

      "respond with a valid etag response in header" in {

        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(OK).withHeader("ETag", "4"))
        )

        val result = await(sut.getEtag(nino.nino).value)
        val resp = result.getOrElse(fail("Either should be Right"))
        resp.status mustBe OK
        resp.header("ETag") mustBe Some("4")
      }

      "respond to a 400 by propagating BAD REQUEST" in {
        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        val result = await(sut.getEtag(nino.nino).value)
        val resp = result.left.getOrElse(fail("Expected a Left"))
        resp.status mustBe BAD_REQUEST
      }

      "respond to a 404 by propagating NOT FOUND" in {

        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(NOT_FOUND))
        )

        val result = await(sut.getEtag(nino.nino).value)
        val resp = result.left.getOrElse(fail("Either should be Left"))
        resp.status mustBe NOT_FOUND
      }

      "Respond to another 4xx error being reported by the httpGet instance by propagating the response code" in {

        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(LOCKED))
        )

        val result = await(sut.getEtag(nino.nino).value)
        val resp = result.left.getOrElse(fail("Either should be Left"))
        resp.status mustBe LOCKED
      }

      "Respond to another 5xx error being reported by the httpGet instance by propagating the response code" in {

        server.stubFor(
          get(url)
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )

        val result = await(sut.getEtag(nino.nino).value)
        val resp = result.left.getOrElse(fail("Either should be Left"))
        resp.status mustBe INTERNAL_SERVER_ERROR
      }

      "Respond to 504 GATEWAY TIMEOUT exception by propagating the response code" in {

        val mockClient = mock[HttpClientV2]
        val mockRequestBuilder = mock[RequestBuilder]
        val connector =
          new CitizenDetailsConnector(mockClient, appConfig, auditLog, metrics)

        when(mockClient.get(any())(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](using any(), any()))
          .thenReturn(Future.failed(new GatewayTimeoutException("Citizen details timeout")))

        val result = await(connector.getEtag(nino.nino).value)
        val resp = result.left.getOrElse(fail("Either should be Left"))
        resp.status mustBe GATEWAY_TIMEOUT
      }
    }
  }
}
