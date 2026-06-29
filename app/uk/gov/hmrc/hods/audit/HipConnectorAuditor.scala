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

package uk.gov.hmrc.hods.audit

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.hods.model.nps.HipNpsPrintSuppressionUpdateRequest
import uk.gov.hmrc.hods.util.AuditLog
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.EventTypes

class HipConnectorAuditor(auditLog: AuditLog) {
  private val CorrelationIdKey = "CorrelationId"

  def createSuccessAuditEvent(
    nino: Nino,
    printSuppressionUpdate: HipNpsPrintSuppressionUpdateRequest,
    postUrl: String,
    maybeCorrelationId: Option[String]
  )(implicit hc: HeaderCarrier): Unit =
    auditLog.createAuditEvent(
      "Got Suppression data successfully sent to NPS",
      "eps-hods-adapter",
      EventTypes.Succeeded,
      "N/A",
      Map("transactionName" -> "Suppression Data sent to NPS", "path" -> postUrl),
      sendSuppressionData(nino.value, printSuppressionUpdate, maybeCorrelationId)
    )

  def createFailedAuditEvent(
    nino: Nino,
    printSuppressionUpdate: HipNpsPrintSuppressionUpdateRequest,
    postUrl: String,
    errorStr: String,
    maybeCorrelationId: Option[String]
  )(implicit hc: HeaderCarrier): Unit =
    auditLog.createAuditEvent(
      "Got Suppression data was not sent to NPS",
      "eps-hods-adapter",
      EventTypes.Failed,
      "N/A",
      Map("transactionName" -> "Suppression Data not sent to NPS", "path" -> postUrl),
      sendSuppressionData(nino.value, printSuppressionUpdate, maybeCorrelationId) ++
        Map("response" -> errorStr)
    )

  private def sendSuppressionData(
    nino: String,
    printSuppressionUpdate: HipNpsPrintSuppressionUpdateRequest,
    maybeCorrelationId: Option[String]
  ): Map[String, String] = {
    val data = Map(
      "via"         -> "hip",
      "nino"        -> nino,
      "printStatus" -> printSuppressionUpdate.printPreferences.mkString(", "),
      "bounced"     -> printSuppressionUpdate.bouncedFlag.toString
    )
    maybeCorrelationId.fold(data)(corrId => data + (CorrelationIdKey -> corrId))
  }
}
