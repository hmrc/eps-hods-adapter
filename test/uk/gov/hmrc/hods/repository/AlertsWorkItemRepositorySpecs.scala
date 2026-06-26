/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.repository

import org.mongodb.scala.model.{ Filters, Updates }
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.{ BeforeAndAfterEach, LoneElement }
import play.api.test.Helpers.*
import uk.gov.hmrc.hods.model.nps.{ Alert, Identifier, NpsAlert }
import uk.gov.hmrc.hods.util.BaseSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{ Failed, ToDo }
import uk.gov.hmrc.mongo.workitem.{ ProcessingStatus, WorkItem }
import org.mongodb.scala.{ Document, ObservableFuture, SingleObservableFuture }
import org.mongodb.scala.documentToUntypedDocument
import uk.gov.hmrc.domain.NinoGenerator

import java.time.temporal.ChronoUnit
import java.time.{ Duration, Instant }
import scala.util.Try

class AlertsWorkItemRepositorySpecs
    extends BaseSpec with MongoSupport with BeforeAndAfterEach with ScalaFutures with IntegrationPatience
    with LoneElement {

  private trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val now: Instant = Instant.now()
    val later: Instant = Instant.now().plus(1, ChronoUnit.DAYS)

    def alertWorkItemRepository: AlertWorkItemRepository =
      new AlertWorkItemRepository(appConfig, mongoComponent) {
        override val inProgressRetryAfter: Duration = Duration.ofMillis(100)
      }

    alertWorkItemRepository.collection
      .deleteMany(Filters.empty())
      .toFuture()
      .futureValue
  }

  "capture Notification" should {

    "Add a new  Email notification" in new Setup {
      val res: WorkItem[Alert] =
        await(alertWorkItemRepository.alertNotification(Alert(NpsAlert(Identifier("nino", nino.nino), "nps", "0004"))))
      val savedItem: WorkItem[Alert] =
        alertWorkItemRepository.collection
          .find()
          .toFuture()
          .futureValue
          .loneElement
      res.item mustBe savedItem.item
    }

    "Add new Email Notification in list" in new Setup {
      await(alertWorkItemRepository.alertNotification(Alert(NpsAlert(Identifier("nino1", nino.nino), "nps", "0004"))))
      await(alertWorkItemRepository.alertNotification(Alert(NpsAlert(Identifier("nino2", nino.nino), "nps", "0005"))))
      val savedItem: Seq[WorkItem[Alert]] =
        alertWorkItemRepository.collection.find().toFuture().futureValue
      savedItem.length mustBe 2
    }

    "Pull new alert items" in new Setup {
      await(alertWorkItemRepository.alertNotification(Alert(NpsAlert(Identifier("nino1", nino.nino), "nps", "0004"))))
      val pullItem =
        alertWorkItemRepository.pullOutstandingItems(now, later).futureValue
      pullItem.get.item mustBe Alert(NpsAlert(Identifier("nino1", nino.nino), "nps", "0004"))
    }

    "retrieve the latest alert when asked" in new Setup {
      // create old alert
      await(alertWorkItemRepository.alertNotification(Alert(NpsAlert(Identifier("nino", nino.nino), "nps", "0004"))))
      val oldAlert = await(alertWorkItemRepository.getLatestAlertForNino(Identifier("nino", nino.nino)))
      oldAlert.get.status mustBe ToDo
      // mark old alert to distinguish it from new alert
      val id =
        await(alertWorkItemRepository.collection.find().toFuture()).head.id
      await(alertWorkItemRepository.markAs(id, Failed))
      val changedAlert = await(alertWorkItemRepository.getLatestAlertForNino(Identifier("nino", nino.nino)))
      changedAlert.get.status mustBe Failed
      // createNewAlert
      await(alertWorkItemRepository.alertNotification(Alert(NpsAlert(Identifier("nino", nino.nino), "nps", "0004"))))
      val newAlert = await(alertWorkItemRepository.getLatestAlertForNino(Identifier("nino", nino.nino)))

      newAlert.get.status mustBe ToDo
    }
  }

  "invalid alerts json" should {
    "return None if unable to parse alerts" in new Setup {
      await(alertWorkItemRepository.alertNotification(Alert(NpsAlert(Identifier("nino", nino.nino), "nps", "0004"))))

      alertWorkItemRepository.collection
        .updateOne(
          Filters.eq("alerts.alert.identifier.value", nino.nino),
          Updates.set("alerts.alert.identifier.value", null)
        )
        .toFuture()
        .futureValue

      val alert: Option[WorkItem[Alert]] =
        await(alertWorkItemRepository.pullOutstandingItems(now, later))
      alert mustBe None
    }
  }

  "stats" should {
    "return count by status for latest updated items" in new Setup {
      val generator = NinoGenerator()
      val nino1 = generator.nextNino
      val nino2 = generator.nextNino
      val nino3 = generator.nextNino
      await(alertWorkItemRepository.alertNotification(Alert(NpsAlert(Identifier("nino1", nino1.nino), "nps", "0004"))))

      await(
        alertWorkItemRepository.alertNotification(
          Alert(NpsAlert(Identifier("nino2", nino2.nino), "nps", "0004")),
          _ => ProcessingStatus.InProgress
        )
      )

      await(
        alertWorkItemRepository.alertNotification(
          Alert(NpsAlert(Identifier("nino3", nino3.nino), "nps", "0004")),
          _ => ProcessingStatus.Failed
        )
      )

      alertWorkItemRepository.collection
        .updateOne(
          Filters.eq("alerts.alert.identifier.value", nino2.nino),
          Updates.set("modifiedDetails.lastUpdated", now.minus(1, ChronoUnit.DAYS))
        )
        .toFuture()
        .futureValue

      val alert = await(alertWorkItemRepository.countByStatus)
      alert mustBe Map("todo" -> 1, "failed" -> 1)
    }
  }

  "indexes" should {
    "match the expected number of total size" in new Setup {
      alertWorkItemRepository.collection.listIndexes().toFuture().futureValue must have size 5
    }

    "set the 'ttl-duration' in the 'createdAtTTLIndex' as defined in the configuration" in new Setup {
      val indexList = alertWorkItemRepository.collection.listIndexes().toFuture().futureValue
      val index: Document = indexList.find(_.getString("name") == "createdAtTTLIndex").get
      val ttl =
        Try(index("expireAfterSeconds").asInt32().getValue).getOrElse(index("expireAfterSeconds").asInt64().getValue)
      ttl must be(604800) // 7 days in seconds
    }
  }
}
