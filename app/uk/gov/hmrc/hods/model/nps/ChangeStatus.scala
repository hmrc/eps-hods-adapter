/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.model.nps

import play.api.libs.json.{ Format, Json, OFormat, Reads }
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import java.time.Instant

case class ChangeStatus(status: ProcessingStatus, availableAt: Option[Instant])

object ChangeStatus {
  implicit val processingStatusReads: Format[ProcessingStatus] =
    ProcessingStatus.Implicits.format
  implicit val instantRead: Reads[Instant] = Reads.DefaultInstantReads
  implicit val formats: OFormat[ChangeStatus] = Json.format[ChangeStatus]
}
