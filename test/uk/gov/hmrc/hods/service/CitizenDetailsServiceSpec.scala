/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.service

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.{ PatienceConfiguration, ScalaFutures }
import play.api.test.Helpers.*
import uk.gov.hmrc.hods.connectors.{ CitizenDetailsConnector, HipConnector }
import uk.gov.hmrc.hods.model.nps.{ Alert, Identifier, NpsAlert, NpsPrintSuppressionUpdateRequest }
import uk.gov.hmrc.hods.util.BaseSpec
import uk.gov.hmrc.http.*

import scala.concurrent.Future

class CitizenDetailsServiceSpec extends BaseSpec with ScalaFutures with PatienceConfiguration {

  val mockCitizenDetailsConnector: CitizenDetailsConnector =
    mock[CitizenDetailsConnector]

  val hipConnector: HipConnector = mock[HipConnector]

  def sut =
    new CitizenDetailsService(mockCitizenDetailsConnector, hipConnector)

  val alert1: Alert = Alert(NpsAlert(Identifier("nino", nino.withoutSuffix), "nps", "0004"))

  trait Setup {

    def getPersonResponse: Future[HttpResponse] =
      Future.successful(HttpResponse(OK, s"""{"nino":"$nino"}"""))

    def cidUpdateResponse: EitherT[Future, HttpResponse, HttpResponse] =
      EitherT.rightT[Future, HttpResponse](HttpResponse(OK, """{"etag":"5"}"""))

    def npsResponse: EitherT[Future, HttpResponse, HttpResponse] =
      EitherT.rightT[Future, HttpResponse](HttpResponse(OK, """{"updatedOptimisticLock": 6}"""))

    when(mockCitizenDetailsConnector.getPersonDetails(any)(any))
      .thenReturn(getPersonResponse)

    when(mockCitizenDetailsConnector.getEtag(any, any)(any))
      .thenReturn(cidUpdateResponse)

    when(hipConnector.updatePrintSuppression(any, any, any)(any, any))
      .thenReturn(npsResponse)
  }

  "CitizenDetailsService - retrieve person" should {

    "return success response for a NINO without suffix, appends temporary suffix" in new Setup {
      val response: HttpResponse = await(sut.retrievePerson(alert1.alert.identifier.value))

      response.status mustBe OK
      response.json.toString must include(nino.nino)
    }

    "return success response for valid NINO" in new Setup {
      val response: HttpResponse = await(sut.retrievePerson(nino.nino))

      response.status mustBe OK
      response.json.toString must include(nino.nino)
    }

    "propagate up an UpstreamError exception with a status of LOCKED" in new Setup {

      override def getPersonResponse: Future[HttpResponse] =
        Future.failed(UpstreamErrorResponse("Locked", LOCKED, LOCKED))

      val response: Future[HttpResponse] = sut.retrievePerson(nino.nino)
      ScalaFutures.whenReady(response.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
        e match {
          case upstreamError: UpstreamErrorResponse =>
            upstreamError.statusCode mustBe LOCKED
          case _ => fail("Expected UpstreamErrorResponse")
        }
      }
    }

    "propagate up a Not Found exception" in new Setup {

      override def getPersonResponse: Future[HttpResponse] =
        Future.failed(new NotFoundException("Not found"))

      val response: Future[HttpResponse] = sut.retrievePerson(nino.nino)
      ScalaFutures.whenReady(response.failed) { e =>
        e mustBe a[NotFoundException]
      }
    }

    "propagate up a Bad Request exception" in new Setup {

      override def getPersonResponse: Future[HttpResponse] =
        Future.failed(new BadRequestException("Oops"))

      val response: Future[HttpResponse] = sut.retrievePerson(nino.nino)
      ScalaFutures.whenReady(response.failed) { e =>
        e mustBe a[BadRequestException]
      }
    }

    "propagate up an exception that is neither NotFound or Locked" in new Setup {

      override def getPersonResponse: Future[HttpResponse] =
        Future.failed(UpstreamErrorResponse("Forbidden", FORBIDDEN, FORBIDDEN))
      val response: Future[HttpResponse] = sut.retrievePerson(nino.nino)
      ScalaFutures.whenReady(response.failed) { e =>
        e mustBe a[UpstreamErrorResponse]
      }
    }

  }

  val mockNpsPrintSuppressionUpdateRequest: NpsPrintSuppressionUpdateRequest =
    NpsPrintSuppressionUpdateRequest("formType", "outputPreference", false)

  "CitizenDetailsService - updatePrintSuppression" should {

    "return 404" in new Setup {
      override def cidUpdateResponse: EitherT[Future, HttpResponse, HttpResponse] =
        EitherT.leftT[Future, HttpResponse](HttpResponse(NOT_FOUND, ""))

      val response: Future[HttpResponse] =
        sut.updatePrintSuppression(nino, mockNpsPrintSuppressionUpdateRequest)
      response.futureValue.status mustBe NOT_FOUND
    }

    "return 423" in new Setup {
      override def cidUpdateResponse: EitherT[Future, HttpResponse, HttpResponse] =
        EitherT.leftT[Future, HttpResponse](HttpResponse(LOCKED, ""))

      val response: Future[HttpResponse] =
        sut.updatePrintSuppression(nino, mockNpsPrintSuppressionUpdateRequest)
      response.futureValue.status mustBe LOCKED
    }

    "get version from response" in {
      val version = sut.getVersionFromResponse(HttpResponse(OK, """{"etag":"5"}"""))
      version mustBe "5"
    }

    "return success" in new Setup {
      when(hipConnector.updatePrintSuppression(any, any, any)(any, any))
        .thenReturn(npsResponse)

      val response: Future[HttpResponse] =
        sut.updatePrintSuppression(nino, mockNpsPrintSuppressionUpdateRequest)
      response.futureValue.status mustBe OK
    }

  }

}
