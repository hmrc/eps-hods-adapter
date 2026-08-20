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

package model.nps

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsNumber, JsResultException, JsString, Json }
import uk.gov.hmrc.hods.model.nps.{ Alert, AlertParameter, Identifier, NpsAlert }
import uk.gov.hmrc.hods.model.nps.NoticeType
import uk.gov.hmrc.hods.model.nps.NoticeType.{ CY, CY_PLUS_1 }

class AlertSpec extends PlaySpec {

  "AlertParameter.format" should {
    import AlertParameter.format

    "read the json correctly" in new TestCase {
      Json.parse(alertParameterJsonString).as[AlertParameter] mustBe alertParameterOb
    }

    "throw exception for incorrect json" in new TestCase {
      intercept[RuntimeException] {
        Json.parse(invalidAlertParameterJsonString).as[AlertParameter]
      }
    }

    "write the json correctly" in new TestCase {
      Json.toJson(alertParameterOb) mustBe Json.parse(alertParameterJsonString)
    }
  }

  "Identifier.format" should {
    import Identifier.format

    "read the json correctly" in new TestCase {
      Json.parse(identifierJsonString).as[Identifier] mustBe identifierOb
    }

    "throw exception for incorrect json" in new TestCase {
      intercept[RuntimeException] {
        Json.parse(invalidIdentifierJsonString).as[Identifier]
      }
    }

    "write the json correctly" in new TestCase {
      Json.toJson(identifierOb) mustBe Json.parse(identifierJsonString)
    }
  }

  "NoticeType" should {
    import NoticeType.format

    "read the correct enum value" in {
      JsString("cy").as[NoticeType] mustBe CY
      JsString("cy_plus_1").as[NoticeType] mustBe CY_PLUS_1
      JsString("CY").as[NoticeType] mustBe CY
      JsString("CY_PLUS_1").as[NoticeType] mustBe CY_PLUS_1
    }

    "throw JsError for invalid string value" in {
      intercept[JsResultException] {
        JsString("cy2").as[NoticeType]
      }.errors.head._2.head.message mustBe "Unknown NoticeType: cy2"

      intercept[JsResultException] {
        JsString("UNKNOWN").as[NoticeType]
      }.errors.head._2.head.message mustBe "Unknown NoticeType: UNKNOWN"
    }

    "throw JsError for value other than String" in {
      intercept[JsResultException] {
        JsNumber(BigDecimal(5.0)).as[NoticeType]
      }.errors.head._2.head.message mustBe "NoticeType must be a string"
    }

    "write the correct value" in {
      Json.toJson(CY) mustBe JsString("cy")
      Json.toJson(CY_PLUS_1) mustBe JsString("cy_plus_1")
    }
  }

  "NpsAlert.format" should {
    import NpsAlert.format

    "read the json correctly" in new TestCase {
      Json.parse(npsAlertJsonString).as[NpsAlert] mustBe npsAlertOb
    }

    "throw exception for invalid notice_type" in new TestCase {
      intercept[RuntimeException] {
        Json.parse(npsAlertWithInvalidNoticeTypeJsonString).as[NpsAlert]
      }
    }

    "throw exception for incorrect json" in new TestCase {
      intercept[RuntimeException] {
        Json.parse(invalidNpsAlertJsonString).as[NpsAlert]
      }
    }

    "write the json correctly" in new TestCase {
      Json.toJson(npsAlertOb) mustBe Json.parse(npsAlertJsonString)
    }
  }

  "Alert.format" should {
    import Alert.format

    "read the json correctly" in new TestCase {
      Json.parse(alertJsonString).as[Alert] mustBe alertOb
      Json.parse(alertWithoutNoticeTypeAndTaxYearJsonString).as[Alert] mustBe alertObWithoutNoticeTypeAndTaxYearOb
    }

    "throw exception for incorrect json" in new TestCase {
      intercept[RuntimeException] {
        Json.parse(invalidAlertJsonString).as[Alert]
      }
    }

    "write the json correctly" in new TestCase {
      Json.toJson(alertOb) mustBe Json.parse(alertJsonString)
      Json.toJson(alertObWithoutNoticeTypeAndTaxYearOb) mustBe Json.parse(alertWithoutNoticeTypeAndTaxYearJsonString)
    }
  }

  trait TestCase {
    val alertParameterJsonString = """{"taxYear":"2026"}"""
    val invalidAlertParameterJsonString = """{"year":"2026"}"""
    val alertParameterOb = AlertParameter("2026")

    val identifierJsonString = """{"id_type":"nino", "value":"AA000003"}"""
    val invalidIdentifierJsonString = """{"id_type":"nino"}"""
    val identifierOb = Identifier("nino", "AA000003")

    val npsAlertJsonString: String =
      """{
        |"identifier":{"id_type":"nino", "value":"AA000003"},
        |"hod_id": "nps",
        |"template_id":"4"
        |}""".stripMargin

    val invalidNpsAlertJsonString: String = """{
                                              |"identifier":{"id_type":"nino", "value":"AA000003"},
                                              |"template_id":"4"
                                              |}""".stripMargin

    val npsAlertWithInvalidNoticeTypeJsonString: String = """{
                                                            |"identifier":{"id_type":"nino", "value":"AA000003"},
                                                            |"template_id":"4",
                                                            |"hod_id": "nps",
                                                            |"notice_type": "UNKNOWN"
                                                            |}""".stripMargin

    val npsAlertOb = NpsAlert(identifier = identifierOb, hod_id = "nps", template_id = "4")

    val npsAlertWithNoticeTypeAndTaxYearOb = NpsAlert(
      identifier = identifierOb,
      hod_id = "nps",
      template_id = "4",
      notice_type = Some(CY_PLUS_1),
      parameters = Some(alertParameterOb)
    )

    val alertJsonString: String = """{
                                    |"alert": {
                                    |"hod_id": "nps",
                                    |"identifier":{ "id_type": "nino", "value": "AA000003" },
                                    |"parameters":{ "taxYear": "2026" },
                                    |"notice_type": "cy_plus_1",
                                    |"template_id": "4"
                                    |}
                                    |}""".stripMargin

    val alertWithoutNoticeTypeAndTaxYearJsonString: String =
      """{
        |"alert": {
        |"hod_id": "nps",
        |"identifier":{ "id_type": "nino", "value": "AA000003" },
        |"template_id": "4"
        |}
        |}""".stripMargin

    val invalidAlertJsonString: String =
      """{}""".stripMargin

    val alertOb = Alert(npsAlertWithNoticeTypeAndTaxYearOb)
    val alertObWithoutNoticeTypeAndTaxYearOb = Alert(npsAlertOb)
  }
}
