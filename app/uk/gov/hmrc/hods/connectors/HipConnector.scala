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

package uk.gov.hmrc.hods.connectors

import cats.data.EitherT
import play.api.Logging
import play.api.http.ContentTypes
import play.api.http.HeaderNames.*
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.hods.config.AppConfig
import uk.gov.hmrc.hods.metrics.HasMetrics
import uk.gov.hmrc.hods.model.nps.{ HipNpsPrintSuppressionUpdateRequest, NpsPrintSuppressionUpdateRequest, PrintPreference, PrintStatus }
import uk.gov.hmrc.hods.util.AuditLog
import uk.gov.hmrc.hods.util.HttpResponseFormat.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import uk.gov.hmrc.http.{ GatewayTimeoutException, HeaderCarrier, HttpResponse }
import uk.gov.hmrc.http.HttpReads.Implicits.*
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.hods.audit.HipConnectorAuditor
import uk.gov.hmrc.hods.model.nps.OutputFormType.P2

import java.net.URI
import java.time.LocalDate
import java.util.{ Base64, UUID }
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }

class HipConnector @Inject() (
  http: HttpClientV2,
  val metrics: Metrics,
  auditLog: AuditLog,
  appConfig: AppConfig
) extends HasMetrics with Logging {
  private val auditLogger = HipConnectorAuditor(auditLog)

  private val hipUrl = appConfig.hipBaseUrl
  private val postUrl = s"$hipUrl/paye/individual/print-preferences"
  private val originatorId = appConfig.hipOriginatorId

  val defaultVersion = "-1"
  val CorrelationIdKey = "CorrelationId"

  private val mkAuthHeader = {
    val credentials = s"${appConfig.hipClientId}:${appConfig.hipClientSecret}"
    val b64Encoded: String = Base64.getEncoder.encodeToString(credentials.getBytes("UTF-8"))
    s"Basic $b64Encoded"
  }

  private val requestHeaders =
    Seq(
      AUTHORIZATION          -> mkAuthHeader,
      CONTENT_TYPE           -> ContentTypes.JSON,
      "Gov-Uk-Originator-Id" -> originatorId
    )

  def updatePrintSuppression(
    printSuppressionUpdate: HipNpsPrintSuppressionUpdateRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, HttpResponse, HttpResponse] = {

    val nino = printSuppressionUpdate.nationalInsuranceNumber

    val correlationId = UUID.randomUUID().toString
    val headers = requestHeaders :+ (CorrelationIdKey -> correlationId)

    withMetricsTimer("nps-suppression") { timer =>
      EitherT(
        http
          .post(new URI(postUrl).toURL)
          .setHeader(headers*)
          .withBody(Json.toJson(printSuppressionUpdate))
          .execute[HttpResponse]
          .map { response =>
            val result = Either.cond(response.status == OK, response, response)
            result match {
              case Right(_) => handleSuccess(nino, printSuppressionUpdate, postUrl, correlationId, timer)
              case Left(_)  => handleFailed(nino, printSuppressionUpdate, postUrl, response, correlationId, timer)
            }
            result
          }
          .recover { ex =>
            val statusCode = ex match {
              case _: GatewayTimeoutException => GATEWAY_TIMEOUT
              case _                          => INTERNAL_SERVER_ERROR
            }
            handleError(nino, printSuppressionUpdate, correlationId, timer, ex.getMessage)
            Left(HttpResponse(statusCode, ex.getMessage))
          }
      )
    }
  }

  def updatePrintSuppression(
    nino: Nino,
    version: String,
    notifySubscriberRequest: NpsPrintSuppressionUpdateRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, HttpResponse, HttpResponse] =
    Try(convert(version)) match {
      case Failure(exception) =>
        EitherT.leftT[Future, HttpResponse](HttpResponse(BAD_REQUEST, exception.getMessage))
      case Success(value) =>
        updatePrintSuppression(buildHipRequest(nino, value, notifySubscriberRequest))
    }

  private def convert(version: String): Short = {
    val ver = version.toShort
    if (ver < 0) {
      throw new IllegalArgumentException(s"Version string is invalid, it cannot be less than 0 and it was $ver")
    }
    ver
  }

  private def buildHipRequest(
    nino: Nino,
    version: Short,
    notifySubscriberRequest: NpsPrintSuppressionUpdateRequest
  ): HipNpsPrintSuppressionUpdateRequest =
    HipNpsPrintSuppressionUpdateRequest(
      nationalInsuranceNumber = nino,
      bouncedFlag = notifySubscriberRequest.bounced,
      currentOptimisticLock = version,
      printPreferences = Seq(
        PrintPreference(P2, PrintStatus.from(notifySubscriberRequest.outputPreference), LocalDate.now)
      )
    )

  private def handleError(
    nino: Nino,
    printSuppressionUpdate: HipNpsPrintSuppressionUpdateRequest,
    correlationId: Metric,
    timer: MetricsTimer,
    ex: String
  )(implicit hc: HeaderCarrier): Unit = {
    auditLogger.createFailedAuditEvent(nino, printSuppressionUpdate, postUrl, ex, Some(correlationId))
    timer.completeTimerAndIncrementFailedCounter()
    logger.warn(
      s"[HIPConnector][updatePrintSuppression][cid: $correlationId] failed suppression post to " +
        s"NPS for NINO- $nino with GatewayTimeoutException"
    )
  }

  // Audit, metrics and logging for success
  private def handleSuccess(
    nino: Nino,
    printSuppressionUpdate: HipNpsPrintSuppressionUpdateRequest,
    postUrl: String,
    correlationId: String,
    timer: MetricsTimer
  )(implicit hc: HeaderCarrier): Unit = {
    auditLogger.createSuccessAuditEvent(nino, printSuppressionUpdate, postUrl, Some(correlationId))
    timer.completeTimerAndIncrementSuccessCounter()
    logger.info(
      s"[HIPConnector][updatePrintSuppression][cid: $correlationId] Successfully posted to NPS for NINO- $nino"
    )
  }

  // Audit, metrics and logging for failure
  private def handleFailed(
    nino: Nino,
    printSuppressionUpdate: HipNpsPrintSuppressionUpdateRequest,
    postUrl: String,
    response: HttpResponse,
    correlationId: String,
    timer: MetricsTimer
  )(implicit hc: HeaderCarrier): Unit = {
    auditLogger.createFailedAuditEvent(nino, printSuppressionUpdate, postUrl, response.asString, Some(correlationId))
    timer.completeTimerAndIncrementFailedCounter()
    logger.warn(
      s"[HIPConnector][updatePrintSuppression][cid: $correlationId] failed suppression post to " +
        s"NPS for NINO- $nino with status - $response.status"
    )
  }
}
