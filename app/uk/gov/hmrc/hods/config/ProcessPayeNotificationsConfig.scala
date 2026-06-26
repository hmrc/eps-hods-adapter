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
