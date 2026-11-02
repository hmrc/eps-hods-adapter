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

package uk.gov.hmrc.hods.testonly

import com.mongodb.client.result.DeleteResult
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.hods.repository.AlertWorkItemRepository
import uk.gov.hmrc.hods.util.BaseSpec

import scala.concurrent.Future

class TestOnlyControllerSpec extends BaseSpec with BeforeAndAfterEach {
  val mockRepository: AlertWorkItemRepository = mock[AlertWorkItemRepository]

  override protected def beforeEach(): Unit =
    reset(mockRepository)

  def sut: TestOnlyController =
    new TestOnlyController(
      mockRepository,
      cc
    )

  "delete alerts" should {
    "return a success message with deleted count" in {
      when(mockRepository.deleteAlerts()).thenReturn(Future.successful(DeleteResult.acknowledged(2)))

      val alert = sut.deleteAlerts().apply(FakeRequest())
      status(alert) mustBe OK
      contentAsString(alert) mustBe "Alerts Deleted: 2"
    }

    "return not modified when alerts not deleted" in {
      when(mockRepository.deleteAlerts()).thenReturn(Future.successful(DeleteResult.unacknowledged))

      val alert = sut.deleteAlerts().apply(FakeRequest())
      status(alert) mustBe NOT_MODIFIED
    }
  }
}
