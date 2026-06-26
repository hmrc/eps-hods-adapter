/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.controllers

import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc.{ Action, AnyContent, ControllerComponents, Result }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.hods.metrics.HasMetrics
import uk.gov.hmrc.hods.model.nps.NpsPrintSuppressionUpdateRequest.PayeFormType
import uk.gov.hmrc.hods.model.nps.{ NotifySubscriberRequest, NpsPrintSuppressionUpdateRequest }
import uk.gov.hmrc.hods.service.CitizenDetailsService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{ Inject, Named }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class NpsController @Inject() (
  @Named("appName") val appName: String,
  citizenDetailsService: CitizenDetailsService,
  val metrics: Metrics,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with HasMetrics with Logging {

  def getPerson(nino: Nino): Action[AnyContent] = Action.async { implicit request =>
    citizenDetailsService.retrievePerson(nino.value).map { response =>
      response.status match {
        case OK =>
          Ok(response.json)
        case NOT_FOUND =>
          logger.warn(s"[NpsController][getPerson] - Got NOT_FOUND Status for nino:$nino")
          NotFound(response.body)
        case LOCKED =>
          logger.warn(s"[NpsController][getPerson] - Got Locked Status for nino:$nino")
          Locked(response.body)
        case SERVICE_UNAVAILABLE =>
          logger.warn(s"[NpsController][getPerson] - Got NpsException for nino:$nino")
          BadGateway(response.body)
        case _ =>
          logger.warn(s"[NpsController][getPerson] - Got Exception for nino:$nino")
          BadGateway(response.body)
      }
    }
  }

  def updatePrintSuppression(nino: Nino): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      withJsonBody[NpsPrintSuppressionUpdateRequest] { sentObject =>
        withMetricsTimer("nps-response") { timer =>
          citizenDetailsService.updatePrintSuppression(nino, sentObject).map { response =>
            processResponse(nino, timer, response)
          }
        }
      }
    }

  private def processResponse(nino: Nino, timer: MetricsTimer, response: HttpResponse): Result =
    response.status match {
      case OK =>
        timer.completeTimerAndIncrementSuccessCounter()
        Ok(response.body)
      case NOT_FOUND =>
        timer.completeTimerAndIncrementFailedCounter()
        logger.warn(s"[NpsController][updatePrintSuppression] - Got NOT_FOUND Status for nino:$nino")
        NotFound("Got NOT_FOUND Status for nino")
      case UNPROCESSABLE_ENTITY =>
        timer.completeTimerAndIncrementFailedCounter()
        logger.warn(s"[NpsController][updatePrintSuppression] - Got UNPROCESSABLE_ENTITY Status for nino:$nino")
        UnprocessableEntity(response.body)
      case BAD_REQUEST =>
        timer.completeTimerAndIncrementFailedCounter()
        logger.warn(
          s"[NpsController][updatePrintSuppression] - Got Bad Request Status for nino:$nino. Error: ${response.body}"
        )
        BadRequest(response.body)
      case SERVICE_UNAVAILABLE =>
        timer.completeTimerAndIncrementFailedCounter()
        logger.warn(s"[NpsController][updatePrintSuppression] - Got Service Unavailable Status for nino:$nino")
        BadGateway(response.body)
      case LOCKED =>
        timer.completeTimerAndIncrementFailedCounter()
        logger.warn(s"[NpsController][updatePrintSuppression] - Got Locked Status for nino:$nino")
        UnprocessableEntity("This account is locked due to MCI")
      case CONFLICT =>
        timer.completeTimerAndIncrementConflictCounter()
        logger.warn(s"[NpsController][updatePrintSuppression] - Got Conflict Status for nino:$nino")
        Conflict(response.body)
      case _ =>
        timer.completeTimerAndIncrementFailedCounter()
        logger.warn(
          s"[NpsController][updatePrintSuppression] - Got Internal server Status for nino:$nino And " +
            s"Status: ${response.status}, Body: ${response.body}"
        )
        BadGateway(response.body)
    }

  def notifySubscriber(): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      withJsonBody[NotifySubscriberRequest] { requestReceived =>
        withMetricsTimer("nps-response") { timer =>
          Try(Nino(requestReceived.taxIds("nino"))) match {
            case Success(nino) =>
              val printSuppressionUpdateRequest = NpsPrintSuppressionUpdateRequest(
                PayeFormType.p2,
                requestReceived.changedValue.name,
                requestReceived.bounced
              )
              citizenDetailsService
                .updatePrintSuppression(Nino(requestReceived.taxIds("nino")), printSuppressionUpdateRequest)
                .map { response =>
                  processResponse(nino, timer, response)
                }
            case Failure(e) =>
              timer.completeTimerAndIncrementFailedCounter()
              logger.warn(
                s"[NpsController][updatePrintSuppression] - Got Bad Request Status due to error: ${e.getMessage}"
              )
              Future(BadRequest(e.getMessage))
          }
        }
      }
    }
}
