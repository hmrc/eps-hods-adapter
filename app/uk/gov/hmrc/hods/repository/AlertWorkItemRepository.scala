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

package uk.gov.hmrc.hods.repository

import javax.inject.{ Inject, Singleton }
import org.mongodb.scala.bson.BsonDocument
import play.api.Logger
import uk.gov.hmrc.hods.config.AppConfig
import uk.gov.hmrc.hods.model.nps.{ Alert, Identifier }
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{ ProcessingStatus, WorkItem, WorkItemFields, WorkItemRepository }
import org.mongodb.scala.documentToUntypedDocument
import org.mongodb.scala.model.*

import java.time
import java.time.{ Instant, ZoneOffset }
import java.util.concurrent.TimeUnit
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

@Singleton
class AlertWorkItemRepository @Inject() (appConfig: AppConfig, mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends WorkItemRepository[Alert](
      "printSuppressionAlerts", // note that this repository is not related to printSuppressions
      mongo,
      Alert.format,
      WorkItemFields(
        id = "_id",
        receivedAt = "modifiedDetails.createdAt",
        updatedAt = "modifiedDetails.lastUpdated",
        availableAt = "availableAt",
        failureCount = "failures",
        status = "status",
        item = "alerts"
      ),
      extraIndexes = Seq(
        IndexModel(
          Indexes.ascending("modifiedDetails.createdAt"),
          IndexOptions()
            .name("createdAtTTLIndex")
            .unique(false)
            .sparse(false)
            .expireAfter(appConfig.alertItemTTL.toSeconds, TimeUnit.SECONDS)
        )
      )
    ) {

  override def now(): Instant = java.time.Instant.now()
  override val inProgressRetryAfter: time.Duration =
    appConfig.inProgressRetryAfterProperty
  private val logger = Logger(getClass)

  override def ensureIndexes(): Future[Seq[String]] =
    for {
      indexes <- collection.listIndexes().toFuture()
      filtered = indexes.filter { i =>
                   i.getString("name") == "last-updated" || i.getString("name") == "status"
                 }
      _        <- Future.sequence(filtered.map(index => collection.dropIndex(index.getString("name")).toFuture()))
      ensuring <- super.ensureIndexes()
    } yield ensuring

  def alertNotification(alert: Alert, initialState: Alert => ProcessingStatus = _ => ToDo): Future[WorkItem[Alert]] =
    super.pushNew(alert, now(), initialState)

  def pullOutstandingItems(failedBefore: Instant, availableBefore: Instant): Future[Option[WorkItem[Alert]]] =
    super.pullOutstanding(failedBefore, availableBefore).recover { case NonFatal(e) =>
      logger.error(s"Unable to pull workitem ${e.getMessage}")
      None
    }

  def getLatestAlertForNino(id: Identifier): Future[Option[WorkItem[Alert]]] =
    collection
      .find(Filters.eq("alerts.alert.identifier.value", id.value))
      .sort(Sorts.descending("modifiedDetails.lastUpdated"))
      .first()
      .toFuture()
      .map(Option(_))

  def countByStatus(implicit ec: ExecutionContext): Future[Map[String, Int]] = {
    val lastUpdated = now().atZone(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).toInstant
    mongo.database
      .getCollection[BsonDocument](collectionName)
      .aggregate(
        Seq(
          Aggregates.`match`(Filters.gte("modifiedDetails.lastUpdated", lastUpdated)),
          Aggregates.group("$status", Accumulators.sum("count", 1))
        )
      )
      .toFuture()
      .map(
        _.flatMap(doc =>
          Map(
            doc.get("_id").asString().getValue -> doc
              .getNumber("count")
              .intValue()
          )
        ).toMap
      )
  }
}
