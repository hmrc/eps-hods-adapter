/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.metrics

import com.codahale.metrics._
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

trait HasMetrics {

  type Metric = String

  val metricsOperator = new MetricsOperator

  def metrics: Metrics

  lazy val registry = metrics.defaultRegistry

  class MetricsTimer(metric: Metric) {

    val timer = metricsOperator.startTimer(metric)

    def completeTimerAndIncrementSuccessCounter(): Unit = {
      timer.stop()
      metricsOperator.incrementSuccessCounter(metric)
    }

    def completeTimerAndIncrementConflictCounter(): Unit = {
      timer.stop()
      metricsOperator.incrementConflictCounter(metric)
    }

    def completeTimerAndIncrementFailedCounter(): Unit = {
      timer.stop()
      metricsOperator.incrementFailedCounter(metric)
    }
  }

  def withMetricsTimer[T](metric: Metric)(block: MetricsTimer => T): T =
    block(new MetricsTimer(metric))

  class MetricsOperator {

    def startTimer(metric: Metric) = registry.timer(s"$metric-timer").time()
    def stopTimer(context: Timer.Context) = context.stop()
    def incrementSuccessCounter(metric: Metric): Unit =
      registry.counter(s"$metric-success-counter").inc()
    def incrementConflictCounter(metric: Metric): Unit =
      registry.counter(s"$metric-conflict-counter").inc()
    def incrementFailedCounter(metric: Metric): Unit =
      registry.counter(s"$metric-failed-counter").inc()
  }
}
