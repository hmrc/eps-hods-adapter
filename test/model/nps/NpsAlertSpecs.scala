/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package model.nps

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.hods.model.nps.{ Alert, Identifier, NpsAlert }

class NpsAlertSpecs extends PlaySpec {

  val identifier = new Identifier("idType1", "value1")
  "NpsAlert" should {

    "write an object of itself to json" in {

      val npsAlert = NpsAlert(identifier, "hodId", "templateId")
      val npsAlertJson = Json.toJson(npsAlert)

      val alert = Alert(npsAlert)
      val jsonNpsAlert = Json.obj("alert" -> npsAlertJson)
      NpsAlert.format.writes(npsAlert) mustBe npsAlertJson
      Alert.format.writes(alert) mustBe jsonNpsAlert
    }
  }
}
