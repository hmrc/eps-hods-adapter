/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.controllers

import org.mongodb.scala.bson.ObjectId
import play.api.Logging
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, ControllerComponents }
import uk.gov.hmrc.hods.metrics.HasMetrics
import uk.gov.hmrc.hods.model.nps.{ Alert, ChangeStatus }
import uk.gov.hmrc.hods.repository.AlertWorkItemRepository
import uk.gov.hmrc.hods.util.AuditLog
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{ ResultStatus, WorkItem }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

class AlertController @Inject() (
  alertWorkItemRepository: AlertWorkItemRepository,
  auditLog: AuditLog,
  val metrics: Metrics,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with HasMetrics with Logging {

  def putAlert: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[Alert] { alert =>
      withMetricsTimer("putalert") { timer =>
        // NPS sends the alert with Nino having no suffix.
        alertWorkItemRepository.alertNotification(alert).map { workItem =>
          auditLog.createAuditEvent(workItem.id.toString, workItem.item.alert, "created")
          timer.completeTimerAndIncrementSuccessCounter()
          Accepted
        }
      }.recover { case NonFatal(e) =>
        logger.warn(
          s"Problem occurred while put Alert with NINO ${alert.alert.identifier.value}, error: ${e.getMessage}"
        )
        InternalServerError(e.getMessage)
      }
    }
  }

  def changeStatus(id: ObjectId): Action[JsValue] = Action.async(parse.json) { implicit request =>
    def result(matched: Boolean) = if (matched) NoContent else Forbidden
    withJsonBody[ChangeStatus] {
      case ChangeStatus(status: ResultStatus, None) =>
        alertWorkItemRepository.findById(id).flatMap {
          case Some(item) =>
            auditLog.createAuditEvent(item.id.toString, item.item.alert, status.name)
            alertWorkItemRepository.complete(id, status).map(result)
          case None => Future.successful(NotFound)
        }
      case ChangeStatus(ToDo, Some(availableAt)) =>
        alertWorkItemRepository
          .markAs(id, ToDo, Some(Instant.ofEpochMilli(availableAt.toEpochMilli)))
          .map(result)
      case _ =>
        Future.successful(BadRequest)
    }
  }

  def status(id: ObjectId): Action[AnyContent] = Action.async {
    alertWorkItemRepository.findById(id).map {
      case Some(n) => Ok(Json.obj("status" -> n.status.name))
      case None    => NotFound
    }
  }
}
