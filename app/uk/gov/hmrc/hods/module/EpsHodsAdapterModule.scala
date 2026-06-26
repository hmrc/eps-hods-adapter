/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.module

import com.google.inject.{ AbstractModule, Provides }
import org.apache.pekko.stream.scaladsl.Sink
import play.api.{ Configuration, Environment }
import uk.gov.hmrc.hods.scheduled.ProcessPayeNotificationsScheduledJob

import javax.inject.Inject
import scala.annotation.unused

class EpsHodsAdapterModule @Inject() (@unused env: Environment, @unused config: Configuration) extends AbstractModule {

  override def configure(): Unit =
    bind(classOf[ProcessPayeNotificationsScheduledJob]).asEagerSingleton()

  @Provides
  def sink(): Sink[Unit, ?] = Sink.ignore
}
