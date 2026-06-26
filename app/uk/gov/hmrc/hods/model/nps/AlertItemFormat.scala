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

import org.mongodb.scala.bson.ObjectId
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.Implicits.format
import uk.gov.hmrc.mongo.workitem.{ ProcessingStatus, WorkItem }

import java.time.format.DateTimeFormatter
import java.time.{ Instant, LocalDateTime, ZoneOffset }
import scala.util.Try

object AlertItemFormat {

  def workItemRestFormat[T](implicit tFormat: Format[T]): Format[WorkItem[T]] = {
    val restBsonIdFormat: Format[ObjectId] = Format(
      Reads.StringReads.map((a: String) => new ObjectId(a)),
      Writes(id => JsString(id.toString))
    )

    val dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    val instantReads: Reads[Instant] = new Reads[Instant] {
      override def reads(json: JsValue): JsResult[Instant] =
        json match {
          case JsString(s) =>
            Try {
              JsSuccess(
                LocalDateTime
                  .parse(s, dateTimeFormatter)
                  .toInstant(ZoneOffset.UTC)
              )
            }.getOrElse {
              JsError(s"Could not parse $s as a Instant with format ${dateTimeFormatter.toString}")
            }
          case _ =>
            JsError(s"Expected value to be a string, was actually $json")
        }
    }

    val instantWrites: Writes[Instant] = Writes[Instant] { i =>
      JsString(LocalDateTime.ofInstant(i, ZoneOffset.UTC).format(dateTimeFormatter))
    }

    workItemFormat(restBsonIdFormat, Format(instantReads, instantWrites), tFormat)
  }

  private def workItemFormat[T](implicit
    idFormat: Format[ObjectId],
    instantFormat: Format[Instant],
    tFormat: Format[T]
  ): Format[WorkItem[T]] = {
    val reads = (
      (__ \ "id").read[ObjectId] and
        (__ \ "modifiedDetails" \ "createdAt").read[Instant] and
        (__ \ "modifiedDetails" \ "lastUpdated").read[Instant] and
        ((__ \ "availableAt")
          .read[Instant] or (__ \ "modifiedDetails" \ "createdAt")
          .read[Instant]) and
        (__ \ "status").read[ProcessingStatus] and
        (__ \ "failures").read[Int].orElse(Reads.pure(0)) and
        (__ \ "alerts").read[T]
    )(WorkItem.apply[T])

    val writes: Writes[WorkItem[T]] = (
      (__ \ "id").write[ObjectId] and
        (__ \ "modifiedDetails" \ "createdAt").write[Instant] and
        (__ \ "modifiedDetails" \ "lastUpdated").write[Instant] and
        (__ \ "availableAt").write[Instant] and
        (__ \ "status").write[ProcessingStatus] and
        (__ \ "failures").write[Int] and
        (__ \ "alerts").write[T]
    )(item => Tuple.fromProductTyped(item))

    Format(reads, writes)
  }
}
