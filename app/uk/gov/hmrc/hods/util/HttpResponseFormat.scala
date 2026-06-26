/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.util

import uk.gov.hmrc.http.HttpResponse

object HttpResponseFormat {

  implicit class HttpResponseString(val r: HttpResponse) {

    def asString: String = {
      val status = r.status
      val body = Option(r.body).getOrElse("")
      val headers = r.headers.mkString(", ")
      s"status: [$status], body: [$body], headers: [$headers]"
    }
  }

}
