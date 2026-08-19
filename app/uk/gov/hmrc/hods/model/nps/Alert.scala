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

import play.api.libs.json.{ Format, JsError, JsString, JsSuccess, JsValue, Json, OFormat, Reads, Writes }

case class AlertParameter(taxYear: String)

object AlertParameter {
  implicit val format: OFormat[AlertParameter] = Json.format[AlertParameter]
}

case class Identifier(id_type: String, value: String)

object Identifier {
  implicit val format: OFormat[Identifier] = Json.format[Identifier]
}

enum NoticeType {
  case CY, CY_PLUS_1
  private def entryName: String = this.toString.toLowerCase
}

object NoticeType {

  implicit val reads: Reads[NoticeType] = Reads {
    case JsString(value) =>
      values.find(_.entryName.equalsIgnoreCase(value)) match {
        case Some(status) => JsSuccess(status)
        case _            => JsError(s"Unknown NoticeType: $value")
      }

    case _ => JsError("NoticeType must be a string")
  }

  implicit val writes: Writes[NoticeType] = Writes(status => JsString(status.entryName))
  implicit val format: Format[NoticeType] = Format(reads, writes)
}

case class NpsAlert(
  identifier: Identifier,
  hod_id: String,
  template_id: String,
  notice_type: Option[NoticeType] = None,
  parameters: Option[AlertParameter] = None
)

object NpsAlert {
  implicit val format: OFormat[NpsAlert] = Json.format[NpsAlert]
}

case class Alert(alert: NpsAlert)

object Alert {
  implicit val format: OFormat[Alert] = Json.format[Alert]
}
