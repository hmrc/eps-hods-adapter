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

package uk.gov.hmrc.hods.service

import javax.inject.Inject
import play.api.Logging
import play.api.http.Status.*
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.hods.connectors.{ CitizenDetailsConnector, HipConnector }
import uk.gov.hmrc.hods.model.nps.*
import uk.gov.hmrc.http.*

import scala.concurrent.{ ExecutionContext, Future }

class CitizenDetailsService @Inject() (
  citizenDetailsConnector: CitizenDetailsConnector,
  hipConnector: HipConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  val defaultVersion = "-1"

  def retrievePerson(nino: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    citizenDetailsConnector.getPersonDetails(nino)(hc) map { response =>
      logger.info(s"[CitizenDetailsService][retrievePerson]Personal details record retrieved for user: $nino")
      response
    } recover {
      case e: UpstreamErrorResponse if e.statusCode == LOCKED =>
        logger.warn(
          s"[CitizenDetailsService][retrievePerson]Personal details record in citizen-details was hidden for user: $nino"
        )
        throw e
      case nf: NotFoundException =>
        logger.warn(
          s"[CitizenDetailsService][retrievePerson]Unable to find personal details record in citizen-details for user: $nino"
        )
        throw nf
      case ex =>
        logger.warn(
          s"[CitizenDetailsService][retrievePerson]Exception occurred retrieving personal details record for user: $nino"
        )
        throw ex
    }

  private def httpResponseEtagError(nino: Nino) = {
    def httpResponseEtagErrorResult: PartialFunction[HttpResponse, HttpResponse] = {

      case response if response.status == NOT_FOUND =>
        logger.warn(
          s"[CitizenDetailsService][updatePrintSuppression(getVersion)] Data not found from NPS  while getting getVersion for NINO- $nino"
        )
        response
      case response if response.status == LOCKED =>
        logger.warn(
          s"[CitizenDetailsService][updatePrintSuppression(getVersion)] LOCKED status found from NPS  while getting getVersion for NINO- $nino"
        )
        response
      case response =>
        logger.warn(
          s"[CitizenDetailsService][updatePrintSuppression(getVersion)] Other status found from NPS  while getting getVersion for NINO- " +
            s"$nino with status ${response.status}"
        )
        response
    }
    httpResponseEtagErrorResult
  }

  def updatePrintSuppression(nino: Nino, printSuppressionUpdate: NpsPrintSuppressionUpdateRequest)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val pf = httpResponseEtagError(nino)

    val result = for {
      eTag <- citizenDetailsConnector.getEtag(nino.value).leftMap(pf)
      version = getVersionFromResponse(eTag)
      res <- hipConnector.updatePrintSuppression(nino, version, printSuppressionUpdate)
    } yield res
    result.merge
  }

  def getVersionFromResponse(httpResponse: HttpResponse): String =
    httpResponse.json.as[Etag].etag.getOrElse(defaultVersion)
}
