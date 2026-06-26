/*
 * Copyright 2025 HM Revenue & Customs
 *
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
