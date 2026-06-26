/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.config

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.hods.util.SimpleObjectBinder

object NinoBinder extends SimpleObjectBinder[Nino](Nino.apply, _.nino) {}
