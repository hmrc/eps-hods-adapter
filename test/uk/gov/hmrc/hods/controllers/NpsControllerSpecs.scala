/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.hods.model.nps.NpsPrintSuppressionUpdateRequest
import uk.gov.hmrc.hods.service.CitizenDetailsService
import uk.gov.hmrc.hods.util.BaseSpec
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

import scala.concurrent.Future

class NpsControllerSpecs extends BaseSpec {

  val mockCitizenDetailsService: CitizenDetailsService =
    mock[CitizenDetailsService]

  def sut: NpsController = new NpsController(
    "eps-hods-adapter",
    mockCitizenDetailsService,
    metrics,
    cc
  )

  "getPerson" should {

    val responses = List(
      (OK, HttpResponse(OK, """{"a":1}""")),
      (NOT_FOUND, HttpResponse(NOT_FOUND, "NOT FOUND")),
      (LOCKED, HttpResponse(LOCKED, "LOCKED")),
      (BAD_GATEWAY, HttpResponse(SERVICE_UNAVAILABLE, "SERVICE DOWN")),
      (BAD_GATEWAY, HttpResponse(INTERNAL_SERVER_ERROR, "ERROR"))
    )

    for ((expected, response) <- responses)
      s"return a status of $expected when ${response.status} is received from cid" in {
        when(mockCitizenDetailsService.retrievePerson(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(response))

        val resStatus: Future[Result] = sut.getPerson(nino)(FakeRequest())
        status(resStatus) mustBe expected
      }
  }

  "updatePrintSupression" should {

    val responses = List(
      (OK, HttpResponse(OK, "OK response body")),
      (NOT_FOUND, HttpResponse(NOT_FOUND, "Not found response body")),
      (UNPROCESSABLE_ENTITY, HttpResponse(UNPROCESSABLE_ENTITY, "Unprocessable Entity response body")),
      (BAD_REQUEST, HttpResponse(BAD_REQUEST, "Bad request response body")),
      (BAD_GATEWAY, HttpResponse(SERVICE_UNAVAILABLE, "SERVICE UNAVAILABLE")),
      (UNPROCESSABLE_ENTITY, HttpResponse(LOCKED, "LOCKED NO PERM LOCK FAIL")),
      (CONFLICT, HttpResponse(CONFLICT, "CONFLICT")),
      (BAD_GATEWAY, HttpResponse(INTERNAL_SERVER_ERROR, "ERROR"))
    )

    for ((expected, response) <- responses)
      s"return a status of $expected when ${response.status} is received from cid" in {

        when(
          mockCitizenDetailsService.updatePrintSuppression(any[Nino], any[NpsPrintSuppressionUpdateRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(response))

        val resStatus: Future[Result] = sut.updatePrintSuppression(nino)(
          FakeRequest()
            .withBody(Json.toJson(NpsPrintSuppressionUpdateRequest("", "", bounced = true)))
        )
        status(resStatus) mustBe expected
      }

    "return a status of 422 when 423 is received from cid and perm lock has failed" in {
      when(
        mockCitizenDetailsService.updatePrintSuppression(any[Nino], any[NpsPrintSuppressionUpdateRequest])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(HttpResponse(LOCKED, "LOCKED PERM LOCK FAILED")))

      val resStatus: Future[Result] = sut.updatePrintSuppression(nino)(
        FakeRequest()
          .withBody(Json.toJson(NpsPrintSuppressionUpdateRequest("", "", bounced = true)))
      )
      status(resStatus) mustBe UNPROCESSABLE_ENTITY
    }
  }

  "notifySubscriber" should {

    val responses = List(
      (OK, HttpResponse(OK, "OK response body")),
      (NOT_FOUND, HttpResponse(NOT_FOUND, "Not found response body")),
      (UNPROCESSABLE_ENTITY, HttpResponse(UNPROCESSABLE_ENTITY, "Unprocessable Entity response body")),
      (BAD_REQUEST, HttpResponse(BAD_REQUEST, "Bad request response body")),
      (BAD_GATEWAY, HttpResponse(SERVICE_UNAVAILABLE, "SERVICE UNAVAILABLE")),
      (UNPROCESSABLE_ENTITY, HttpResponse(LOCKED, "LOCKED NO PERM LOCK FAIL")),
      (CONFLICT, HttpResponse(CONFLICT, "CONFLICT")),
      (BAD_GATEWAY, HttpResponse(INTERNAL_SERVER_ERROR, "ERROR"))
    )

    val request =
      FakeRequest().withBody(Json.parse(s"""{
                                           |  "changedValue" : "digital",
                                           |  "updatedAt"    : "2023-10-11T01:30:00.000Z",
                                           |  "taxIds"       :  { "nino" : "AB123456C", "utr" : "1234567890"},
                                           |  "bounced"      : false
                                           |}
                                           |""".stripMargin))

    for ((expected, response) <- responses)
      s"return a status of $expected when ${response.status} is received from cid" in {

        when(
          mockCitizenDetailsService.updatePrintSuppression(any[Nino], any[NpsPrintSuppressionUpdateRequest])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(response))

        val resStatus: Future[Result] = sut.notifySubscriber()(request)
        status(resStatus) mustBe expected
      }

    "return a status of 422 when 423 is received from cid and perm lock has failed" in {
      when(
        mockCitizenDetailsService.updatePrintSuppression(any[Nino], any[NpsPrintSuppressionUpdateRequest])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(HttpResponse(LOCKED, "LOCKED PERM LOCK FAILED")))

      val resStatus: Future[Result] = sut.notifySubscriber()(request)
      status(resStatus) mustBe UNPROCESSABLE_ENTITY
    }

    "return a BAD_REQUEST status when invalid json body received" in {
      val resStatus: Future[Result] =
        sut.notifySubscriber()(FakeRequest().withBody(Json.obj("test" -> "invalid")))
      status(resStatus) mustBe BAD_REQUEST
      contentAsString(resStatus) must include("Invalid NotifySubscriberRequest payload:")
    }

    "return a BAD_REQUEST status when a valid json body without nino is received" in {
      val request: FakeRequest[JsValue] =
        FakeRequest().withBody(Json.parse(s"""{
                                             |  "changedValue" : "digital",
                                             |  "updatedAt"    : "2023-10-11T01:30:00.000Z",
                                             |  "taxIds"       :  { "saUtr" : "1234567890"},
                                             |  "bounced"      : false
                                             |}
                                             |""".stripMargin))
      val resStatus: Future[Result] = sut.notifySubscriber()(request)
      status(resStatus) mustBe BAD_REQUEST
      contentAsString(resStatus) must include("key not found: nino")
    }
  }
}
