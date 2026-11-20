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

package uk.gov.hmrc.hods

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer
import org.scalactic.source.Position
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.{ JsObject, JsString }
import play.api.mvc.{ Headers, RequestHeader }
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{ Authorization, HeaderCarrier }
import uk.gov.hmrc.http.hooks.Data
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.{ ControllerConfig, ControllerConfigs, DefaultHttpAuditEvent }
import uk.gov.hmrc.play.bootstrap.filters.Details

import scala.concurrent.ExecutionContext.Implicits.global

class AuditFilterSpec extends PlaySpec with ScalaFutures with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("HC-Authorization")))
  val configuration: Configuration = Configuration(
    "microservice.services.customs-data-store.host" -> "host",
    "bootstrap.auditing.maxBodyLength"              -> 32665
  )
  val controllerConfigs = ControllerConfigs(Map("testController" -> ControllerConfig()))
  val auditConnector = mock[AuditConnector]
  val httpAuditEvent = new DefaultHttpAuditEvent("test")
  val materializer: Materializer = NoMaterializer

  trait RequestDetails {
    def getBuildRequestDetails(requestHeader: RequestHeader, requestBody: Data[String]): Details = ???
  }

  val auditFilter: AuditFilter & RequestDetails =
    new AuditFilter(configuration, controllerConfigs, auditConnector, httpAuditEvent, materializer)
      with RequestDetails {
      override def getBuildRequestDetails(requestHeader: RequestHeader, requestBody: Data[String]): Details =
        buildRequestDetails(requestHeader, requestBody)
    }

  "AuditFilter" must {
    "extendedDataEvent should include correlationId and requestBody nullifying authorization in the request and header carrier" in {
      val headers =
        Headers(
          "requestBody"   -> "test-body",
          "CorrelationId" -> "test-correlationId",
          "Authorization" -> "Header Authorization Value"
        ).headers
      val requestDetails: JsObject = JsObject(
        headers.map { case (key, value) => key -> JsString(value) }
      )

      val event = auditFilter.extendedDataEvent(
        "test",
        "test-transaction",
        FakeRequest("GET", "/test"),
        requestDetails
      )
      val details = event.detail.as[Map[String, String]]

      details mustBe Map(
        "method"          -> "GET",
        "CorrelationId"   -> "test-correlationId",
        "deviceID"        -> "-",
        "referrer"        -> "-",
        "Authorization"   -> "-",
        "input"           -> "Request to /test",
        "userAgentString" -> "-",
        "requestBody"     -> "test-body",
        "ipAddress"       -> "-"
      )
    }

    "buildRequestDetails" must {
      def testBuildRequestDetails(headers: Headers, expectedCorrelationId: String)(implicit
        position: Position
      ) = {
        val request = FakeRequest(
          "GET",
          "/test",
          headers,
          "body"
        )

        val requestDetails =
          auditFilter.getBuildRequestDetails(request, Data("test-body", isTruncated = false, isRedacted = false))
        val details = requestDetails.details.as[Map[String, String]]

        details mustBe Map("requestBody" -> "test-body", "CorrelationId" -> expectedCorrelationId)
      }

      "generate details with passed correlationId and requestBody from the given request header" in {
        val headers =
          Headers("requestBody" -> "test-body", "CorrelationId" -> "test-correlationId", "Authorization" -> "ss")

        testBuildRequestDetails(headers, "test-correlationId")
      }

      "generate details with a generated correlationId when not passed and requestBody from the given request header" in {
        val headers = Headers("requestBody" -> "test-body", "Authorization" -> "ss")

        testBuildRequestDetails(headers, "-")
      }

    }
  }
}
