/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.model.nps

import play.api.libs.json._

final case class Etag(etag: Option[String])
object Etag {
  implicit val formats: OFormat[Etag] = Json.format[Etag]
}
