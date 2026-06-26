/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.util

import org.apache.pekko.stream.Materializer
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.stubControllerComponents
import play.api.test.Injecting
import uk.gov.hmrc.domain.{ Nino, NinoGenerator }
import uk.gov.hmrc.hods.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ HeaderCarrier, RequestId, SessionId }
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.concurrent.ExecutionContext

trait BaseSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite with Injecting {

  implicit override lazy val app: Application =
    new GuiceApplicationBuilder().build()

  lazy val http: HttpClientV2 = inject[HttpClientV2]
  lazy val appConfig: AppConfig = inject[AppConfig]
  lazy val auditLog: AuditLog = inject[AuditLog]
  lazy val metrics: Metrics = inject[Metrics]
  implicit lazy val ec: ExecutionContext = inject[ExecutionContext]
  implicit lazy val mat: Materializer = app.materializer

  val sessionId = "testSessionId"
  val requestId = "testRequestId"

  implicit val hc: HeaderCarrier = HeaderCarrier(
    sessionId = Some(SessionId(sessionId)),
    requestId = Some(RequestId(requestId))
  )

  val nino: Nino = NinoGenerator().nextNino

  val cc: ControllerComponents = stubControllerComponents()
}
