/*
 * Copyright 2026 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.scheduled

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{ times, verify, verifyNoMoreInteractions, when }
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.hods.config.ProcessPayeNotificationsConfig
import uk.gov.hmrc.hods.service.AlertWorkItemService

import scala.concurrent.Future

class ProcessPayeNotificationsScheduledJobSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  val testKit = ActorTestKit()
  implicit val system: ActorSystem = testKit.system.classicSystem
  implicit lazy val materializer: Materializer = Materializer(system)

  "ProcessPayeNotificationsScheduledJob" should {

    "emits elements correctly" in new Setup {
      when(mockService.processOutstandingItemAsUnit(any))
        .thenReturn(Future.successful(()))

      scheduledJob.start()
      probeSubscriber
        .request(2)
        .expectNext(())
        .expectNext(())
    }

    "respect configured delays and intervals" in new Setup {
      when(mockService.processOutstandingItemAsUnit(any))
        .thenReturn(Future.successful(()))

      val startTime = System.currentTimeMillis()
      scheduledJob.start()

      probeSubscriber
        .request(2)
        .expectNext(()) // Should arrive after ~100ms

      val firstElementTime = System.currentTimeMillis()
      (firstElementTime - startTime) must be >= 100L

      probeSubscriber
        .expectNext(()) // Should arrive after another ~200ms

      val secondElementTime = System.currentTimeMillis()
      (secondElementTime - firstElementTime) must be >= 180L // Give it some leeway
    }

    "successfully call service during workload processing" in new Setup {
      when(mockService.processOutstandingItemAsUnit(any))
        .thenReturn(Future.successful(()))

      scheduledJob.start()

      probeSubscriber
        .request(2)
        .expectNext(())
        .expectNext(())

      // Verify external service was called
      verify(mockService, times(2)).processOutstandingItemAsUnit(any)
      verifyNoMoreInteractions(mockService)
    }

  }

  trait Setup {
    val configuration = Configuration(
      "removeOlderCollections.durationInDays"                -> 1,
      "scheduling.processPayeNotifications.retryFailedAfter" -> "2.0",
      "scheduling.processPayeNotifications.initialDelay"     -> "100 milliseconds",
      "scheduling.processPayeNotifications.interval"         -> "200 milliseconds",
      "scheduling.processPayeNotifications.taskEnabled"      -> true
    )
    val mockService: AlertWorkItemService = mock[AlertWorkItemService]
    val lifecycle = mock[ApplicationLifecycle]
    val failedAfter: Long = 5L

    val (probeSubscriber, probeSink) = TestSink.probe[Unit].preMaterialize()

    val scheduledJob: ProcessPayeNotificationsScheduledJob =
      new ProcessPayeNotificationsScheduledJob(
        mockService,
        lifecycle,
        ProcessPayeNotificationsConfig(configuration),
        sink = probeSink
      )
  }
}
