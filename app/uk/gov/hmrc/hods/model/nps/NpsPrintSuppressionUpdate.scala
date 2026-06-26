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

package uk.gov.hmrc.hods.model.nps

import play.api.libs.json.{ JsValue, Json, OFormat, Writes }

case class NpsPrintSuppressionUpdateRequest(formType: String, outputPreference: String, bounced: Boolean)

object NpsPrintSuppressionUpdateRequest {
  implicit val formats: OFormat[NpsPrintSuppressionUpdateRequest] =
    Json.format[NpsPrintSuppressionUpdateRequest]
  implicit val formatUpdate: Writes[NpsPrintSuppressionUpdateRequest] =
    new Writes[NpsPrintSuppressionUpdateRequest] {
      def writes(printSuppressionUpdate: NpsPrintSuppressionUpdateRequest): JsValue =
        Json.toJson(printSuppressionUpdate)
    }

  object PayeFormType {
    val p2 = "P2"
  }

  object OutputPreference {
    val paper = "paper"
    val digital = "digital"
  }
}

case class NpsPrintSuppressionUpdateResponse(rejectionCode: Int)

object NpsPrintSuppressionUpdateResponse {
  implicit val formats: OFormat[NpsPrintSuppressionUpdateResponse] =
    Json.format[NpsPrintSuppressionUpdateResponse]
  implicit val formatUpdate: Writes[NpsPrintSuppressionUpdateResponse] =
    new Writes[NpsPrintSuppressionUpdateResponse] {
      def writes(printSuppressionUpdateResponse: NpsPrintSuppressionUpdateResponse): JsValue =
        Json.toJson(printSuppressionUpdateResponse)
    }
}
