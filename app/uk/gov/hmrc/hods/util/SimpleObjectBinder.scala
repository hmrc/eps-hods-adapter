/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods.util

/** Created by akhileshkumar on 16/11/2017.
  */
import play.api.mvc.PathBindable

import scala.reflect.ClassTag

class SimpleObjectBinder[T](bind: String => T, unbind: T => String)(implicit ct: ClassTag[T]) extends PathBindable[T] {
  override def bind(key: String, value: String): Either[String, T] =
    try
      Right(bind(value))
    catch {
      case _: Throwable =>
        Left(s"Cannot parse parameter '$key' with value '$value' as '${ct.runtimeClass.getSimpleName}'")
    }

  def unbind(key: String, value: T): String = unbind(value)
}
