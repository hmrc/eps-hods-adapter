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
