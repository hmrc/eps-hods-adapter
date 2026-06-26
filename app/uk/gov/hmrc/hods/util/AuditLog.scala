/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.util

import uk.gov.hmrc.hods.model.nps.NpsAlert
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.{ DataEvent, EventTypes }

import javax.inject.Inject
import scala.concurrent.duration.*
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps

class AuditLog @Inject() (
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext) {

  def createAuditEvent(
    transactionName: String,
    auditSource: String,
    auditType: String,
    path: String = "N/A",
    tags: Map[String, String],
    details: Map[String, String]
  )(implicit hc: HeaderCarrier): Unit = {

    val event = DataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = AuditExtensions
        .auditHeaderCarrier(hc)
        .toAuditTags(transactionName, path) ++ tags,
      detail = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails(details.toSeq*)
    )
    Await.result[AuditResult](auditConnector.sendEvent(event), 10 seconds): Unit
  }

  def createAuditEvent(
    alertId: String,
    notificationAlert: NpsAlert,
    tag: String
  ): Future[AuditResult] =
    auditConnector.sendEvent(
      DataEvent(
        auditSource = "eps-hods-adapter",
        auditType = EventTypes.Succeeded,
        tags = Map("transactionName" -> s"Print suppression notification $tag"),
        detail = Map(
          "alertId"    -> alertId,
          "idType"     -> notificationAlert.identifier.id_type,
          "idValue"    -> notificationAlert.identifier.value,
          "hodId"      -> notificationAlert.hod_id,
          "templateId" -> notificationAlert.template_id
        )
      )
    )
}
