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

import play.api.libs.json.{ JsError, JsString, JsSuccess, Json, Reads }

import java.time.Instant
import scala.util.Try

sealed abstract class MessageDeliveryFormat(val name: String)
object MessageDeliveryFormat {
  case object Paper extends MessageDeliveryFormat(name = "paper")

  case object Digital extends MessageDeliveryFormat(name = "digital")

  implicit val reads: Reads[MessageDeliveryFormat] =
    Reads[MessageDeliveryFormat] {
      case JsString(value) if value == Paper.name   => JsSuccess(Paper)
      case JsString(value) if value == Digital.name => JsSuccess(Digital)
      case _                                        => JsError("Invalid message delivery format")
    }
}

case class NotifySubscriberRequest(
  changedValue: MessageDeliveryFormat,
  updatedAt: Instant,
  taxIds: Map[String, String],
  bounced: Boolean
)

object NotifySubscriberRequest {
  implicit val instantReads: Reads[Instant] = {
    case JsString(s) =>
      Try(Instant.parse(s))
        .fold(
          _ => JsError(s"Could not parse $s as an ISO Instant"),
          JsSuccess.apply(_)
        )
    case json =>
      JsError(s"Expected value to be a string, was actually $json")
  }
  implicit val reads: Reads[NotifySubscriberRequest] =
    Json.reads[NotifySubscriberRequest]
}
