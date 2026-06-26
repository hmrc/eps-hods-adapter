/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.util

import org.scalatestplus.play.PlaySpec
import play.api.http.MimeTypes
import play.api.http.Status.OK
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.hods.util.HttpResponseFormat._

class HttpResponseFormatSpec extends PlaySpec {

  "HttpResponse" should {
    "format status correctly" in {
      val r: HttpResponse = HttpResponse.apply(OK, "", Map.empty)
      r.asString must equal("status: [200], body: [], headers: []")
    }

    "format body correctly" in {
      val r: HttpResponse = HttpResponse.apply(OK, "{\"stat\": true}", Map.empty)
      r.asString must equal("status: [200], body: [{\"stat\": true}], headers: []")
    }

    "format headers correctly" in {
      val r: HttpResponse =
        HttpResponse.apply(OK, "", Map(CONTENT_TYPE -> Seq(MimeTypes.JSON)))
      r.asString must equal("status: [200], body: [], headers: [Content-Type -> List(application/json)]")
    }
  }
}
