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

package uk.gov.hmrc.hods.controllers

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.mongodb.scala.bson.ObjectId
import org.scalactic.source.Position
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.*
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

  "putAlert - receiving an alert from nps" should {

    "return 202 for a success" when {
      def testSuccess(npsAlert: NpsAlert)(implicit position: Position) = {
        val requestData = Json.toJson(Alert(npsAlert))
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

        val eventualResult = sut.putAlert(request)

        status(eventualResult) mustBe ACCEPTED
        contentAsString(eventualResult) mustBe ""

        // NOTE: ACCEPTED state is returned whilst work still continues, i.e. sendEvent does not happen synchronously
        eventually(timeout(2.seconds)) {
          verify(mockAuditConnector).sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])
          succeed
        }
      }

      "there is no notice_type and no tax year exists" in {
        testSuccess(NpsAlert(Identifier("nino", nino.withoutSuffix), "nps", "0004", notice_type = None))
      }

      "the notice_type is not CY_PLUS_1 and no tax year" in {
        testSuccess(NpsAlert(Identifier("nino", nino.withoutSuffix), "nps", "0004", notice_type = Some(NoticeType.CY)))
      }

      "there is no notice_type and a valid tax year" in {
        testSuccess(
          NpsAlert(
            Identifier("nino", nino.withoutSuffix),
            "nps",
            "0004",
            notice_type = None,
            parameters = Some(AlertParameter("1111"))
          )
        )
      }

      "the notice_type is CY_PLUS_1 and the tax year is in the correct YYYY format" in {
        testSuccess(
          NpsAlert(
            Identifier("nino", nino.withoutSuffix),
            "nps",
            "0004",
            notice_type = Some(NoticeType.CY_PLUS_1),
            parameters = Some(AlertParameter("1111"))
          )
        )
      }

      "the notice_type is not CY_PLUS_1 and the tax year is in the correct YYYY format" in {
        testSuccess(
          NpsAlert(
            Identifier("nino", nino.withoutSuffix),
            "nps",
            "0004",
            notice_type = Some(NoticeType.CY),
            parameters = Some(AlertParameter("1111"))
          )
        )
      }

    }

    "return a 422" when {

      "the notice type combination" when {

        def testInvalidNoticeTypeCombination(npsAlert: NpsAlert, errorMessage: String)(implicit position: Position) = {
          val alerts = Alert(npsAlert)
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

          val result = sut.putAlert.apply(request)
          status(result) mustBe UNPROCESSABLE_ENTITY
          contentAsString(result) mustBe errorMessage

        }

        val invalidDateFormatMessage = "Request could not be processed as taxYear must be a four-digit year"

        "the noticeType is CY_PLUS_1 and there is no date" in {
          testInvalidNoticeTypeCombination(
            NpsAlert(Identifier("nino", nino.withoutSuffix), "nps", "0004", notice_type = Some(NoticeType.CY_PLUS_1)),
            "Request could not be processed as taxYear parameter is missing for notice_type CY_PLUS_1"
          )
        }

        "the noticeType is CY_PLUS_1 and there is a date in the wrong format" in {
          testInvalidNoticeTypeCombination(
            NpsAlert(
              Identifier("nino", nino.withoutSuffix),
              "nps",
              "0004",
              notice_type = Some(NoticeType.CY_PLUS_1),
              parameters = Some(AlertParameter("111"))
            ),
            invalidDateFormatMessage
          )
        }

        "the noticeType exists and there is a date in the wrong format" in {
          testInvalidNoticeTypeCombination(
            NpsAlert(
              Identifier("nino", nino.withoutSuffix),
              "nps",
              "0004",
              notice_type = Some(NoticeType.CY),
              parameters = Some(AlertParameter("111"))
            ),
            invalidDateFormatMessage
          )
        }

        "the noticeType is None and there is a date in the wrong format" in {
          testInvalidNoticeTypeCombination(
            NpsAlert(
              Identifier("nino", nino.withoutSuffix),
              "nps",
              "0004",
              notice_type = None,
              parameters = Some(AlertParameter("111"))
            ),
            invalidDateFormatMessage
          )
        }

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

    "return a 400" when {
      "body is not correct json" in {
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

      "json has invalid value of NoticeType" in {
        val alertJsonString =
          """{"alert":{
            |"identifier":{"id_type":"nino","value":"XW446889"},
            |"hod_id":"nps","template_id":"0004",
            |"notice_type":"cy_plus"
            |}
            |}""".stripMargin

        val requestData = Json.toJson(alertJsonString)

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
          requestData
        )

        val result = sut.putAlert.apply(request)

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "changeStatus - The api should respond" should {

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
