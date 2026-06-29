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

case class Identifier(id_type: String, value: String)

object Identifier {
  implicit val format: OFormat[Identifier] = Json.format[Identifier]
}

case class NpsAlert(identifier: Identifier, hod_id: String, template_id: String)

object NpsAlert {
  implicit val format: OFormat[NpsAlert] = Json.format[NpsAlert]
}

case class Alert(alert: NpsAlert)

object Alert {
  implicit val format: OFormat[Alert] = Json.format[Alert]
}
