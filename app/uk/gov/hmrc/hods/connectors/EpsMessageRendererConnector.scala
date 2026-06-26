/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.connectors

import play.api.Logging
import play.api.http.Status
import play.api.libs.json.JsValue
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.hods.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

import java.net.URI
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

class EpsMessageRendererConnector @Inject() (http: HttpClientV2, appConfig: AppConfig)(using ec: ExecutionContext)
    extends Logging {

  private val serviceUrl: String = appConfig.epsMessageRendererBaseUrl

  def postNotification(
    notification: JsValue
  )(using hc: HeaderCarrier): Future[HttpResponse] = {
    val postUrl = s"$serviceUrl/eps-message-renderer/process-notification"
    http
      .post(new URI(postUrl).toURL)
      .withBody(notification)
      .execute[HttpResponse]
      .recover { case NonFatal(ex) =>
        logger.error(
          s"Failed to post notification due to ${ex.getMessage}"
        )
        HttpResponse(Status.GATEWAY_TIMEOUT, ex.getMessage)
      }
  }
}
