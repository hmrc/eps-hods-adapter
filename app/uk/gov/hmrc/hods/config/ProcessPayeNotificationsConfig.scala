/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.config

import play.api.Configuration

import javax.inject.{ Inject, Singleton }
import scala.concurrent.duration.*

@Singleton
class ProcessPayeNotificationsConfig @Inject() (configuration: Configuration) {
  val name = "processPayeNotifications"

  lazy val initialDelay: FiniteDuration =
    configuration.get[FiniteDuration](s"scheduling.$name.initialDelay")

  lazy val interval: FiniteDuration =
    configuration.get[FiniteDuration](s"scheduling.$name.interval")

  lazy val taskEnabled: Boolean =
    configuration.get[Boolean](s"scheduling.$name.taskEnabled")
}
