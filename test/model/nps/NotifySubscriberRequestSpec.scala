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
