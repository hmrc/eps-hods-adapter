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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{ JsString, JsValue, Json, Writes, __ }
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class HipNpsPrintSuppressionUpdateRequest(
  nationalInsuranceNumber: Nino,
  bouncedFlag: Boolean,
  currentOptimisticLock: Short,
  printPreferences: Seq[PrintPreference]
)
object HipNpsPrintSuppressionUpdateRequest {
  private implicit val itemWrites: Writes[Nino] = Nino.ninoWrite

  given writes: Writes[HipNpsPrintSuppressionUpdateRequest] = (
    (__ \ "nationalInsuranceNumber").write[Nino] and
      (__ \ "bouncedFlag").write[Boolean] and
      (__ \ "currentOptimisticLock").write[Short] and
      (__ \ "printPreferences").write[Seq[PrintPreference]]
  )(item => Tuple.fromProductTyped(item))
}

case class PrintPreference(
  outputFormType: OutputFormType,
  printStatus: PrintStatus,
  lastUpdatedDate: LocalDate
)
object PrintPreference {

  implicit val dateWrites: Writes[LocalDate] = new Writes[LocalDate] {
    def writes(date: LocalDate): JsValue =
      JsString(DateTimeFormatter.ISO_LOCAL_DATE.format(date))
  }

  given writes: Writes[PrintPreference] = (
    (__ \ "outputFormType").write[OutputFormType] and
      (__ \ "printStatus").write[PrintStatus] and
      (__ \ "lastUpdatedDate").write[LocalDate]
  )(item => Tuple.fromProductTyped(item))

  implicit val itemWrites: Writes[PrintPreference] = Json.writes[PrintPreference]
  implicit val seqItemWrites: Writes[Seq[PrintPreference]] = Writes.seq[PrintPreference](itemWrites)
}

enum PrintStatus(val value: String) {
  case PAPER extends PrintStatus("PAPER")
  case DIGITAL extends PrintStatus("DIGITAL")
}
object PrintStatus {
  given writes: Writes[PrintStatus] = Writes { printStatus =>
    JsString(printStatus.value)
  }

  def from(messageDeliveryFormat: String): PrintStatus =
    messageDeliveryFormat.toLowerCase match {
      case "paper"   => PAPER
      case "digital" => DIGITAL
    }
}

enum OutputFormType(val value: String) {
  // There are many form types described in the api
  // AFAIK these are the only ones likely to apply to DC
  case NOT_KNOWN extends OutputFormType("NOT KNOWN")
  case P2 extends OutputFormType("P2")
}
object OutputFormType {
  given writes: Writes[OutputFormType] = Writes { outputFormType =>
    JsString(outputFormType.value)
  }
}
