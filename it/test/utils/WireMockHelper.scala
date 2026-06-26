/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, Suite }

trait WireMockHelper extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>

  protected val server: WireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  override def beforeAll(): Unit =
    server.start()

  override def beforeEach(): Unit =
    server.resetAll()

  override def afterAll(): Unit =
    server.stop()
}
