/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.controllers

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.mongodb.scala.bson.ObjectId
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.*
import play.api.mvc.Result
import play.api.test.Helpers.*
import play.api.test.{ FakeHeaders, FakeRequest }
import uk.gov.hmrc.hods.model.nps.*
import uk.gov.hmrc.hods.repository.AlertWorkItemRepository
import uk.gov.hmrc.hods.util.{ AuditLog, BaseSpec }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{ Succeeded, ToDo }
import uk.gov.hmrc.mongo.workitem.{ ProcessingStatus, ResultStatus, WorkItem }
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.DataEvent

import java.time.{ Instant, LocalDateTime, ZoneOffset }
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ ExecutionContext, Future }

class AlertControllerSpec extends BaseSpec with BeforeAndAfterEach with ScalaFutures {
  val mockRepository: AlertWorkItemRepository = mock[AlertWorkItemRepository]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockAudit: AuditLog = new AuditLog(mockAuditConnector)

  val ldt: LocalDateTime = LocalDateTime.of(2015, 5, 1, 13, 7, 2)
  val id: ObjectId = new ObjectId()
  val instant: Instant =
    Instant.ofEpochMilli(ldt.toInstant(ZoneOffset.UTC).toEpochMilli)
  val alertItem: Alert = Alert(NpsAlert(Identifier("nino", nino.nino), "nps", "0004"))
  val workItem: WorkItem[Alert] =
    new WorkItem[Alert](id, instant, instant, instant, ToDo, 1, alertItem)

  override protected def beforeEach(): Unit = {
    reset(mockRepository)
    reset(mockAuditConnector)
  }

  def sut: AlertController =
    new AlertController(
      mockRepository,
      mockAudit,
      metrics,
      cc
    )

  "receiving an alert from nps" should {
    "return a 202 and success message if the body is correct" in {
      val alert1 =
        Alert(NpsAlert(Identifier("nino", nino.withoutSuffix), "nps", "0004"))
      val alerts = alert1
      val requestData = Json.toJson(alerts)
      val request = FakeRequest(
        "PUT",
        "/nps/person/alert",
        FakeHeaders(
          scala.Seq(
            "Content-type" -> "application/json"
          )
        ),
        requestData
      )
      when(mockAuditConnector.sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(AuditResult.Success))
      when(mockRepository.alertNotification(any[Alert], any()))
        .thenReturn(Future.successful(workItem))
      val alert: Result = sut.putAlert(request).futureValue
      alert.header.status mustBe ACCEPTED

      // NOTE: ACCEPTED state is returned whilst work still continues, i.e. sendEvent does not happen synchronously
      eventually(timeout(2.seconds)) {
        verify(mockAuditConnector).sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])
        succeed
      }
    }

    "return a status as INTERNAL_SERVER_ERROR if alert can not be created" in {
      val requestData = Json.toJson(Alert(NpsAlert(Identifier("nino", nino.withoutSuffix), "nps", "0004")))
      val request = FakeRequest(
        "PUT",
        "/nps/person/alert",
        FakeHeaders(
          Seq(
            "Content-type" -> "application/json"
          )
        ),
        requestData
      )

      when(mockRepository.alertNotification(any[Alert], any()))
        .thenReturn(Future.failed(Exception("Failed to create alert")))

      val alert = sut.putAlert.apply(request)
      status(alert) mustBe INTERNAL_SERVER_ERROR
    }

    "return a 400 if the body is not json" in {
      val requestData = "identifier: nino"
      val request = FakeRequest(
        "PUT",
        "/nps/person/send-test-alert",
        FakeHeaders(
          Seq(
            "Content-type"         -> "application/json",
            "ETag"                 -> "1",
            "X-TXID"               -> "1234",
            "Gov-Uk-Originator-Id" -> "HMRC_HODS_ADAPTER"
          )
        ),
        Json.toJson(requestData)
      )

      val alert = sut.putAlert.apply(request)

      status(alert) mustBe BAD_REQUEST
    }
  }

  "The api should respond" should {

    "return 403 if status is ToDo but result is forbidden for changeStatus API" in {
      val availableAt =
        LocalDateTime.of(2015, 8, 10, 13, 7, 2).toInstant(ZoneOffset.UTC)
      val processStatus = ToDo
      val changeStatus = ChangeStatus(processStatus, Some(availableAt))
      when(mockRepository.findById(any[ObjectId])) `thenReturn` Future
        .successful(Some(workItem))

      when(mockRepository.complete(any[ObjectId], any[ResultStatus])) `thenReturn` Future
        .successful(false)

      when(mockRepository.markAs(any[ObjectId], any[ProcessingStatus], any[Option[Instant]])) `thenReturn` Future
        .successful(false)

      val requestData = Json.toJson(changeStatus)
      val request = FakeRequest(
        "POST",
        s"/preferences/alert/print-suppression/$id/status",
        FakeHeaders(
          Seq(
            "Content-type"         -> "application/json",
            "ETag"                 -> "1",
            "X-TXID"               -> "1234",
            "Gov-Uk-Originator-Id" -> "HMRC_HODS_ADAPTER"
          )
        ),
        requestData
      )
      val resStatus = sut.changeStatus(id).apply(request)

      status(resStatus) mustBe 403
    }

    "return 404 if id is not found for changeStatus API" in {

      when(mockRepository.findById(any[ObjectId])) `thenReturn` Future
        .successful(None)

      val requestData = Json.parse("""{"status":"succeeded"}""")
      val request = FakeRequest(
        "POST",
        s"/preferences/alert/print-suppression/$id/status",
        FakeHeaders(
          Seq(
            "Content-type"         -> "application/json",
            "ETag"                 -> "1",
            "X-TXID"               -> "1234",
            "Gov-Uk-Originator-Id" -> "HMRC_HODS_ADAPTER"
          )
        ),
        requestData
      )
      val resStatus = sut.changeStatus(id).apply(request)

      status(resStatus) mustBe 404
    }

    "return 400 if bad request for changeStatus API" in {
      val availableAt =
        LocalDateTime.of(2015, 8, 10, 13, 7, 2).toInstant(ZoneOffset.UTC)
      val processStatus = Succeeded
      val changeStatus = ChangeStatus(processStatus, Some(availableAt))
      val requestData = Json.toJson(changeStatus)
      val request = FakeRequest(
        "POST",
        s"/preferences/alert/print-suppression/$id/status",
        FakeHeaders(
          Seq(
            "Content-type"         -> "application/json",
            "ETag"                 -> "1",
            "X-TXID"               -> "1234",
            "Gov-Uk-Originator-Id" -> "HMRC_HODS_ADAPTER"
          )
        ),
        requestData
      )
      val resStatus = sut.changeStatus(id).apply(request)

      status(resStatus) mustBe 400
    }

    "return 204 if request for changeStatus API succeeded" in {
      val requestData = Json.toJson(ChangeStatus(Succeeded, None))
      when(mockRepository.findById(any)).thenReturn(Future.successful(Some(workItem)))
      when(mockRepository.complete(any[ObjectId], any[ResultStatus])).thenReturn(Future.successful(true))

      val request = FakeRequest(
        "POST",
        s"/preferences/alert/print-suppression/$id/status",
        FakeHeaders(
          Seq(
            "Content-type"         -> "application/json",
            "ETag"                 -> "1",
            "X-TXID"               -> "1234",
            "Gov-Uk-Originator-Id" -> "HMRC_HODS_ADAPTER"
          )
        ),
        requestData
      )
      val resStatus = sut.changeStatus(id).apply(request)

      status(resStatus) mustBe 204

      verify(mockAuditConnector, times(1)).sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])
    }

    "return 200 if successfully received request for status API" in {
      when(mockRepository.findById(any[ObjectId])) `thenReturn` Future
        .successful(Some(workItem))

      val resStatus = sut.status(id).apply(FakeRequest())
      status(resStatus) mustBe OK
    }

    "return 404 if id is not found for status API" in {

      when(mockRepository.findById(any[ObjectId])) `thenReturn` Future
        .successful(None)

      val resStatus = sut.status(id).apply(FakeRequest())
      status(resStatus) mustBe NOT_FOUND

    }
  }
}
