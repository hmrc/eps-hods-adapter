/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.connectors

import com.codahale.metrics.MetricRegistry
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{ reset, verify, when }
import org.mockito.ArgumentMatchers.{ any, argThat }
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration
import play.api.http.Status.{ CONFLICT, FORBIDDEN, OK }
import play.api.libs.json.JsObject
import uk.gov.hmrc.domain.NinoGenerator
import uk.gov.hmrc.hods.config.AppConfig
import uk.gov.hmrc.hods.model.nps.OutputFormType.NOT_KNOWN
import uk.gov.hmrc.hods.model.nps.PrintStatus.DIGITAL
import uk.gov.hmrc.hods.model.nps.{ HipNpsPrintSuppressionUpdateRequest, PrintPreference }
import uk.gov.hmrc.hods.util.AuditLog
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.http.client.{ HttpClientV2, RequestBuilder }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.net.URL
import java.time.LocalDate
import scala.concurrent.Future
import scala.language.postfixOps

class HipConnectorSpec extends AnyFreeSpec with ScalaFutures with BeforeAndAfterEach {

  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockMetrics: Metrics = mock[Metrics]
  val mockAuditLog: AuditLog = mock[AuditLog]

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val configuration = Configuration(
    "microservice.services.nps-hod-hip.host"                -> "localhost",
    "microservice.services.nps-hod-hip.port"                -> "8185",
    "microservice.services.nps-hod-hip.originator-id"       -> "MDTP-DC-TEST",
    "microservice.services.nps-hod-hip.authorization-token" -> "local"
  )

  val servicesConfig = ServicesConfig(configuration)
  val appConfig = AppConfig(configuration, servicesConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient)
    reset(mockRequestBuilder)
    reset(mockMetrics)
    reset(mockAuditLog)

    when(mockMetrics.defaultRegistry).thenReturn(new MetricRegistry)
    when(mockHttpClient.post(any)(any)).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any)(using any, any, any)).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any[(String, String)])).thenReturn(mockRequestBuilder)
  }

  "update print suppression" - {

    "calls the correct url" in {
      when(mockRequestBuilder.execute[HttpResponse](using any, any)).thenReturn(Future.successful(HttpResponse(OK)))

      val connector = HipConnector(mockHttpClient, mockMetrics, mockAuditLog, appConfig)
      val response = connector.updatePrintSuppression(mkRequest).value.futureValue
      val result = response.getOrElse(fail("Must be Right"))
      result.status mustBe OK

      verify(mockHttpClient)
        .post(argThat[URL] { url =>
          url.toString == "http://localhost:8185/paye/individual/print-preferences"
        })(any[HeaderCarrier])
    }

    "calls with the correct body" in {
      when(mockRequestBuilder.execute[HttpResponse](using any, any)).thenReturn(Future.successful(HttpResponse(OK)))

      val connector = HipConnector(mockHttpClient, mockMetrics, mockAuditLog, appConfig)
      val psu: HipNpsPrintSuppressionUpdateRequest = mkRequest

      val response = connector.updatePrintSuppression(psu).value.futureValue
      val result = response.getOrElse(fail("Must be Right"))
      result.status mustBe OK

      verify(mockRequestBuilder)
        .withBody(
          argThat[JsObject] { body =>
            (body \ "nationalInsuranceNumber").as[String] == psu.nationalInsuranceNumber.value &&
            (body \ "bouncedFlag").as[Boolean] == false &&
            (body \ "currentOptimisticLock").as[Short] == 1 &&
            (body \\ "outputFormType").map(_.as[String]).head == "NOT KNOWN" &&
            (body \\ "printStatus").map(_.as[String]).head == "DIGITAL" &&
            (body \\ "lastUpdatedDate").map(_.as[String]).head == "2025-02-10"
          }
        )(using any, any, any)
    }

    "success case is audited" in {
      when(mockRequestBuilder.execute[HttpResponse](using any, any)).thenReturn(Future.successful(HttpResponse(OK)))
      val connector = HipConnector(mockHttpClient, mockMetrics, mockAuditLog, appConfig)
      val response = connector.updatePrintSuppression(mkRequest).value.futureValue
      response.getOrElse(fail("Must be Right")).status mustBe OK
      verify(mockAuditLog).createAuditEvent(any, any, any, any, any, any)(any)
    }

    "handles conflict response as expected" in {
      when(mockRequestBuilder.execute[HttpResponse](using any, any))
        .thenReturn(Future.successful(HttpResponse(CONFLICT)))
      when(mockRequestBuilder.execute[HttpResponse](using any, any)).thenReturn(Future.successful(HttpResponse(OK)))

      val connector = HipConnector(mockHttpClient, mockMetrics, mockAuditLog, appConfig)
      val response = connector.updatePrintSuppression(mkRequest).value.futureValue
      response.getOrElse(fail("Must be Right")).status mustBe OK
    }

    "handles forbidden response as expected" in {
      when(mockRequestBuilder.execute[HttpResponse](using any, any))
        .thenReturn(Future.successful(HttpResponse(FORBIDDEN)))
      val connector = HipConnector(mockHttpClient, mockMetrics, mockAuditLog, appConfig)
      val response = connector.updatePrintSuppression(mkRequest).value.futureValue
      response.left.getOrElse(fail("Must be Left")).status mustBe FORBIDDEN
      verify(mockAuditLog).createAuditEvent(any, any, any, any, any, any)(any)
    }
  }

  private def mkRequest = {
    val nino = NinoGenerator().nextNino

    HipNpsPrintSuppressionUpdateRequest(
      nationalInsuranceNumber = nino,
      bouncedFlag = false,
      currentOptimisticLock = 1,
      printPreferences = Seq(PrintPreference(NOT_KNOWN, DIGITAL, LocalDate.parse("2025-02-10")))
    )
  }
}
