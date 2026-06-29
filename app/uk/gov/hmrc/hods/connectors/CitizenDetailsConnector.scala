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
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import play.api.Logging
import play.api.http.Status
import play.api.http.Status.*
import uk.gov.hmrc.hods.config.AppConfig
import uk.gov.hmrc.hods.metrics.HasMetrics
import uk.gov.hmrc.hods.util.AuditLog
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ GatewayTimeoutException, HeaderCarrier, HttpReads, HttpResponse }
import uk.gov.hmrc.play.audit.model.EventTypes
import uk.gov.hmrc.hods.util.HttpResponseFormat.*

import java.net.URI
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class CitizenDetailsConnector @Inject() (
  http: HttpClientV2,
  appConfig: AppConfig,
  auditLog: AuditLog,
  val metrics: Metrics
)(implicit ec: ExecutionContext)
    extends HasMetrics with Logging {

  lazy val serviceUrl: String = appConfig.citizenDetailsBaseUrl

  val defaultVersion = -1

  def basicPersonUrl(nino: String) =
    s"$serviceUrl/citizen-details/$nino/designatory-details/basic"
  def etagUrl(nino: String) = s"$serviceUrl/citizen-details/$nino/etag"

  private implicit val httpReads: HttpReads[HttpResponse] =
    new HttpReads[HttpResponse] {
      def read(method: String, url: String, response: HttpResponse): HttpResponse =
        response
    }

  def getPersonDetails(nino: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val urlToRead = basicPersonUrl(nino)
    val transactionName = "Get Person Details"
    withMetricsTimer("get-person-details") { timer =>
      http.get(new URI(urlToRead).toURL).execute[HttpResponse].map { response =>
        response.status match {
          case OK =>
            auditEvent(transactionName, EventTypes.Succeeded, urlToRead, Option(OK), response.asString)
            timer.completeTimerAndIncrementSuccessCounter()
            response
          case _ =>
            auditEvent(transactionName, EventTypes.Failed, urlToRead, Option(response.status), response.asString)
            timer.completeTimerAndIncrementFailedCounter()
            response
        }
      }
    }
  }

  def getEtag(nino: String, transactionName: String = "Get ETag")(implicit
    hc: HeaderCarrier
  ): EitherT[Future, HttpResponse, HttpResponse] = {
    val urlToRead = etagUrl(nino)
    withMetricsTimer("get-etag") { timer =>
      EitherT(
        http
          .get(new URI(urlToRead).toURL)
          .execute[HttpResponse]
          .map { response =>
            response.status match {
              case OK =>
                auditEvent(transactionName, EventTypes.Succeeded, urlToRead, Option(OK), response.asString)
                timer.completeTimerAndIncrementSuccessCounter()
                Right(response)
              case _ =>
                auditEvent(transactionName, EventTypes.Failed, urlToRead, Option(response.status), response.asString)
                timer.completeTimerAndIncrementFailedCounter()
                Left(response)
            }
          }
          .recover { case ex: GatewayTimeoutException =>
            auditEvent(transactionName, EventTypes.Failed, urlToRead, None, ex.message)
            timer.completeTimerAndIncrementFailedCounter()
            Left(HttpResponse(Status.GATEWAY_TIMEOUT, ex.message))
          }
      )
    }
  }

  private def auditEvent(
    transactionName: String,
    auditType: String,
    path: String,
    maybeStatus: Option[Int],
    message: String
  )(implicit hc: HeaderCarrier): Unit = {

    val detailsMap: Map[String, String] = maybeStatus match {
      case Some(s) => Map("status" -> s.toString)
      case None    => Map.empty
    }

    auditLog.createAuditEvent(
      transactionName,
      "eps-hods-adapter",
      auditType,
      path,
      Map("transactionName" -> transactionName, "path" -> path),
      detailsMap ++ Map("response" -> message)
    )

  }
}
