/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.audit

import uk.gov.hmrc.play.audit.model.Audit

trait Auditable {

  def appName: String

  def audit: Audit
}
