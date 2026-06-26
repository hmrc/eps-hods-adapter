/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.service

import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{ any, eq as meq }
import org.mockito.Mockito.*
import org.scalatest.concurrent.{ PatienceConfiguration, ScalaFutures }
import play.api.libs.json.JsValue
import play.api.mvc.{ Result, Results }
import play.api.test.Helpers.*
import uk.gov.hmrc.hods.config.AppConfig
import uk.gov.hmrc.hods.connectors.EpsMessageRendererConnector
import uk.gov.hmrc.hods.model.nps.{ Alert, Identifier, NpsAlert }
import uk.gov.hmrc.hods.repository.AlertWorkItemRepository
import uk.gov.hmrc.hods.util.{ AuditLog, BaseSpec }
import uk.gov.hmrc.http.*
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.WorkItem
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

import java.time.Instant
import scala.concurrent.Future

class AlertWorkItemServiceSpec extends BaseSpec with ScalaFutures with PatienceConfiguration {

  val mockEpsMrConnector: EpsMessageRendererConnector =
    mock[EpsMessageRendererConnector]

  val mockRepository: AlertWorkItemRepository =
    mock[AlertWorkItemRepository]

  val mockConfig: AppConfig = mock[AppConfig]

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockAudit: AuditLog = new AuditLog(mockAuditConnector)

  val available: Instant = Instant.now()
  val failedBefore: Instant = available.minusSeconds(10)

  val id = ObjectId()
  val alertItem: Alert = Alert(NpsAlert(Identifier("nino", nino.nino), "nps", "0004"))
  val workItem: WorkItem[Alert] =
    new WorkItem[Alert](id, available, available, available, ToDo, 1, alertItem)

  when(mockConfig.retryFailedNotificationsAfter).thenReturn(10L)

  def sut: AlertWorkItemService =
    new AlertWorkItemService(mockRepository, mockEpsMrConnector, mockConfig, mockAudit) {
      override def availableBefore: Instant = available
    }

  "AlertWorkItemService - process outstanding item" should {

    "return OK response for an item sent to eps message renderer" in {
      given hc: HeaderCarrier = any[HeaderCarrier]
      when(mockRepository.pullOutstandingItems(meq(failedBefore), meq(available)))
        .thenReturn(Future.successful(Some(workItem)))
      when(mockEpsMrConnector.postNotification(any[JsValue]))
        .thenReturn(Future.successful(HttpResponse(ACCEPTED)))
      when(mockAuditConnector.sendEvent(any)(any, any))
        .thenReturn(Future.successful(Success))

      val response: Result = await(sut.processOutstandingItem())

      response mustBe Results.Ok("Alert sent")
    }

    "return error response for an item failed to sent" in {
      given hc: HeaderCarrier = any[HeaderCarrier]

      when(mockRepository.pullOutstandingItems(meq(failedBefore), meq(available)))
        .thenReturn(Future.successful(Some(workItem)))
      when(mockEpsMrConnector.postNotification(any[JsValue]))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
      when(mockAuditConnector.sendEvent(any)(any, any))
        .thenReturn(Future.successful(Success))

      val response: Result = await(sut.processOutstandingItem())

      response mustBe Results.InternalServerError("Error returned from post notification")
    }

    "return NoContent response when no items available to send" in {
      clearInvocations(mockEpsMrConnector, mockAuditConnector)
      when(mockRepository.pullOutstandingItems(any, any))
        .thenReturn(Future.successful(None))

      val response: Result = await(sut.processOutstandingItem())

      response mustBe Results.NoContent
      verifyNoInteractions(mockEpsMrConnector)
      verifyNoInteractions(mockAuditConnector)
    }

    "available before & failed before values to be evaluated when called" in {
      val service: AlertWorkItemService =
        new AlertWorkItemService(mockRepository, mockEpsMrConnector, mockConfig, mockAudit)
      service.availableBefore.isAfter(service.failedBefore)
    }
  }
}
