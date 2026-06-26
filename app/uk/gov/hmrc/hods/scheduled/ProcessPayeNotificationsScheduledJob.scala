/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.scheduled

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{ KillSwitch, KillSwitches, Materializer }
import org.apache.pekko.stream.scaladsl.{ Keep, Sink, Source }
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.hods.config.ProcessPayeNotificationsConfig
import uk.gov.hmrc.hods.service.AlertWorkItemService

import javax.inject.Inject
import scala.concurrent.Future

class ProcessPayeNotificationsScheduledJob @Inject() (
  notificationService: AlertWorkItemService,
  lifecycle: ApplicationLifecycle,
  config: ProcessPayeNotificationsConfig,
  sink: Sink[Unit, ?] = Sink.ignore
)(implicit actorSystem: ActorSystem)
    extends Logging {

  private var killSwitch: Option[KillSwitch] = None

  // Only run the stream if enabled in config
  if (config.taskEnabled) {
    start()
  }

  // Entrypoint
  def start(): Option[KillSwitch] = {
    logger.warn(s"Stream starting: initialDelay: ${config.initialDelay}, interval: ${config.interval}")

    val (killSwitch, streamDone) =
      // Tick source, generates a Unit element to start execution periodically
      Source
        .tick(config.initialDelay, config.interval, tick = ())
        .mapAsync(1) { _ =>
          logger.debug(s"-> Tick")
          notificationService.processOutstandingItemAsUnit
        }
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(sink)(Keep.both)
        .run() // Run forever

    this.killSwitch = Some(killSwitch)

    // Register cleanup on shutdown
    lifecycle.addStopHook { () =>
      logger.warn("Shutting down processing paye notifications stream...")
      killSwitch.shutdown() // Terminate the stream gracefully
      Future.successful(())
    }
    this.killSwitch
  }
}
