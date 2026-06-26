/*
 * Copyright 2023 HM Revenue & Customs
 *
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
