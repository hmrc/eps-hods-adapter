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
