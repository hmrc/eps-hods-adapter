/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.service

import org.bson.types.ObjectId
import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.json.{ Format, JsObject, Json }
import play.api.mvc.Result
import play.api.mvc.Results.{ InternalServerError, NoContent, Ok }
import uk.gov.hmrc.hods.config.AppConfig
import uk.gov.hmrc.hods.connectors.EpsMessageRendererConnector
import uk.gov.hmrc.hods.model.nps.{ Alert, AlertItemFormat }
import uk.gov.hmrc.hods.repository.AlertWorkItemRepository
import uk.gov.hmrc.hods.util.AuditLog
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class AlertWorkItemService @Inject() (
  alertWorkItemRepository: AlertWorkItemRepository,
  epsMessageRendererConnector: EpsMessageRendererConnector,
  appConfig: AppConfig,
  auditLog: AuditLog
)(using ec: ExecutionContext)
    extends Logging {

  private def statusUrlFor(alert: WorkItem[Alert]): String =
    s"/eps-hods-adapter/preferences/alert/print-suppression/${alert.id}/status"
  given format(using tFormat: Format[Alert]): Format[WorkItem[Alert]] =
    AlertItemFormat.workItemRestFormat
  given hc: HeaderCarrier = HeaderCarrier()

  def availableBefore: Instant = Instant.now()
  def failedBefore: Instant = availableBefore.minusSeconds(appConfig.retryFailedNotificationsAfter)

  def processOutstandingItemAsUnit(implicit tFormat: Format[Alert]): Future[Unit] =
    processOutstandingItem().map(_ => ())

  def processOutstandingItem()(implicit
    tFormat: Format[Alert]
  ): Future[Result] =
    alertWorkItemRepository
      .pullOutstandingItems(failedBefore, availableBefore)
      .flatMap {
        case None => Future(NoContent)
        case Some(item) =>
          auditLog.createAuditEvent(item.id.toString, item.item.alert, "progressed")
          val jsonValue = Json.toJson(item).as[JsObject] ++ Json.obj("statusUrl" -> statusUrlFor(item))
          epsMessageRendererConnector.postNotification(jsonValue).map(handleResponse(_, item.id))
      }

  private def handleResponse(response: HttpResponse, alertId: ObjectId) = response.status match {
    case ACCEPTED =>
      val msg = "Alert sent"
      logger.debug(msg)
      Ok(msg)
    case _ =>
      logger.error(s"Error returned from post notification for alertId - $alertId")
      InternalServerError("Error returned from post notification")
  }
}
