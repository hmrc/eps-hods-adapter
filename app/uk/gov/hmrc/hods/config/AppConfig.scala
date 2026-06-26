/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.config

import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.{ Duration, DurationInt, FiniteDuration, SECONDS }
import java.time

@Singleton
class AppConfig @Inject() (val configuration: Configuration, servicesConfig: ServicesConfig) {

  lazy val inProgressRetryAfterProperty: time.Duration =
    configuration.underlying.getDuration("printSuppression.retryInProgressNotificationsAfter")
  lazy val citizenDetailsBaseUrl: String =
    servicesConfig.baseUrl("citizen-details")
  lazy val desBaseUrl: String = servicesConfig.baseUrl("nps-hod-des")
  lazy val desServiceKey: String =
    servicesConfig.getConfString("nps-hod-des.key", "local")
  lazy val desServiceEnvironment: String =
    servicesConfig.getConfString("nps-hod-des.environment", "local")

  lazy val hipBaseUrl: String = servicesConfig.baseUrl("nps-hod-hip")
  lazy val hipOriginatorId: String = servicesConfig.getConfString("nps-hod-hip.originator-id", "local")

  lazy val hipClientId: String = servicesConfig.getConfString("nps-hod-hip.client-id", "id-local")
  lazy val hipClientSecret: String = servicesConfig.getConfString("nps-hod-hip.client-secret", "secret-local")

  private val defaultTTL: Duration = 365.days
  lazy val alertItemTTL: Duration = configuration
    .getOptional[Duration]("printSuppression.alertItemTTL")
    .getOrElse(defaultTTL)
  lazy val epsMessageRendererBaseUrl: String =
    servicesConfig.baseUrl("eps-message-renderer")
  lazy val retryFailedNotificationsAfter: Long =
    configuration.getOptional[Long]("scheduling.processPayeNotifications.retryFailedAfter").getOrElse(1L)
  def getDuration(configKey: String, propertyKey: String): FiniteDuration =
    Duration(
      configuration
        .getOptional[Int](s"scheduling.$configKey.$propertyKey")
        .getOrElse(1), // defaults to 1 seconds
      SECONDS
    )
}
