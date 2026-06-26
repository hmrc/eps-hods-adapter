/*
 * Copyright 2023 HM Revenue & Customs
 *
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
