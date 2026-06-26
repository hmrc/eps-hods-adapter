/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package model.nps

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsResultException, Json }
import uk.gov.hmrc.hods.model.nps.MessageDeliveryFormat.{ Digital, Paper }
import uk.gov.hmrc.hods.model.nps.NotifySubscriberRequest

import java.time.Instant

class NotifySubscriberRequestSpec extends PlaySpec {

  "NotifySubscriberRequest" should {
    "reads from valid json - digital" in {
      val taxIds = Map("nino" -> "AB123456C", "utr" -> "1234567890")
      val jsonRequest =
        Json.parse(s"""{
                      |  "changedValue" : "digital",
                      |  "updatedAt"    : "2023-10-11T01:30:00.000Z",
                      |  "taxIds"       :  { "nino" : "AB123456C", "utr" : "1234567890"},
                      |  "bounced"      : false 
                      |}
                      |""".stripMargin)

      jsonRequest.as[NotifySubscriberRequest] mustBe NotifySubscriberRequest(
        Digital,
        Instant.parse("2023-10-11T01:30:00.000Z"),
        taxIds,
        false
      )
    }

    "reads from valid json - paper" in {
      val taxIds = Map("nino" -> "AB123456C", "utr" -> "1234567890")
      val jsonRequest =
        Json.parse(s"""{
                      |  "changedValue" : "paper",
                      |  "updatedAt"    : "2023-10-11T01:30:00.000Z",
                      |  "taxIds"       :  { "nino" : "AB123456C", "utr" : "1234567890"},
                      |  "bounced"      : true 
                      |}
                      |""".stripMargin)

      jsonRequest.as[NotifySubscriberRequest] mustBe NotifySubscriberRequest(
        Paper,
        Instant.parse("2023-10-11T01:30:00.000Z"),
        taxIds,
        true
      )
    }

    "fails to read from invalid message delivery format value in json" in {
      val jsonRequest =
        Json.parse(s"""{
                      |  "changedValue" : "online",
                      |  "updatedAt"    : "2023-10-11T01:30:00.000Z",
                      |  "taxIds"       :  { "nino" : "AB123456C", "utr" : "1234567890"},
                      |  "bounced"      : false  
                      |}
                      |""".stripMargin)

      assertThrows[JsResultException](jsonRequest.as[NotifySubscriberRequest])
    }

    "fails to read from json missing expected fields" in {
      val jsonRequest =
        Json.parse(s"""{
                      |  "changedValue" : "digital",
                      |  "updatedAt"    : "2023-10-11T01:30:00.000Z"
                      |}
                      |""".stripMargin)

      assertThrows[JsResultException](jsonRequest.as[NotifySubscriberRequest])
    }
  }
}
