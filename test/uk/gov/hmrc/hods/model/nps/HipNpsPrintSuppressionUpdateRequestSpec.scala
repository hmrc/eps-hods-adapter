/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.model.nps

import play.api.libs.json.{ JsArray, JsObject, JsString, Json }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.hods.model.nps.OutputFormType.{ NOT_KNOWN, P2 }
import uk.gov.hmrc.hods.model.nps.PrintStatus.DIGITAL
import uk.gov.hmrc.hods.util.BaseSpec

import java.time.LocalDate

class HipNpsPrintSuppressionUpdateRequestSpec extends BaseSpec {

  "Formatting" should {

    "write printstatus correctly" in {
      val ps = PrintStatus.PAPER
      val json = Json.toJson(ps)
      json mustBe (JsString("PAPER"))
    }

    "write print preference correctly" in {
      import PrintPreference.writes
      val pp = PrintPreference(P2, PrintStatus.DIGITAL, LocalDate.parse("2025-02-07"))
      val json = Json.toJson(pp)

      (json \ "outputFormType").as[String] mustBe "P2"
      (json \ "printStatus").as[String] mustBe "DIGITAL"
      (json \ "lastUpdatedDate").as[String] mustBe "2025-02-07"
    }

    "write the request correctly" in {
      val req = HipNpsPrintSuppressionUpdateRequest(
        Nino("AB001122A"),
        false,
        1,
        Seq(PrintPreference(NOT_KNOWN, DIGITAL, LocalDate.parse("2025-02-07")))
      )
      val json = Json.toJson(req)

      (json \ "nationalInsuranceNumber").as[String] mustBe "AB001122A"
      (json \ "bouncedFlag").as[Boolean] mustBe false
      (json \ "currentOptimisticLock").as[Short] mustBe 1

      val pp = (json \ "printPreferences").as[JsArray]
      val firstEntry = pp(0).as[JsObject]
      firstEntry.toString mustBe
        """{"outputFormType":"NOT KNOWN","printStatus":"DIGITAL","lastUpdatedDate":"2025-02-07"}"""
    }
  }
}
