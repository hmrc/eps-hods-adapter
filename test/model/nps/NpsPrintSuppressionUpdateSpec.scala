/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package model.nps

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.hods.model.nps.NpsPrintSuppressionUpdateRequest.{ OutputPreference, PayeFormType }
import uk.gov.hmrc.hods.model.nps.{ NpsPrintSuppressionUpdateRequest, NpsPrintSuppressionUpdateResponse }

class NpsPrintSuppressionUpdateSpec extends PlaySpec {

  "NpsPrintSuppressionUpdateRequest" should {

    "write an object of itself to json" in {

      val printSuppressionUpdateRequest =
        NpsPrintSuppressionUpdateRequest(PayeFormType.p2, OutputPreference.digital, false)
      val jsonUpdateRequest = Json.obj("formType" -> "P2", "outputPreference" -> "digital", "bounced" -> false)

      NpsPrintSuppressionUpdateRequest.formatUpdate.writes(printSuppressionUpdateRequest) mustBe jsonUpdateRequest
    }

  }

  "NpsPrintSuppressionUpdateResponse" should {

    "have json response object" in {
      val PrintSuppressionResponse = NpsPrintSuppressionUpdateResponse(0)
      val jsonResponseObject = Json.parse(
        s"""
           |{
           |"rejectionCode":0
           |}
         """.stripMargin
      )
      def writeJson = NpsPrintSuppressionUpdateResponse.formatUpdate
      writeJson.writes(PrintSuppressionResponse) mustBe jsonResponseObject
    }
  }

}
