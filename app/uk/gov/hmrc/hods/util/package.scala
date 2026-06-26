/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.hods

import _root_.play.api.mvc.PathBindable
import config.NinoBinder
import org.mongodb.scala.bson.ObjectId

import scala.util.{ Failure, Success, Try }

package object binders {
  implicit val ninoBinder: NinoBinder.type = NinoBinder

  implicit def bsonIdBinder(implicit stringBinder: PathBindable[String]): PathBindable[ObjectId] =
    new PathBindable[ObjectId] {

      def bind(key: String, value: String): Either[String, ObjectId] =
        stringBinder.bind(key, value) match {
          case Left(msg) => Left(msg)
          case Right(id) =>
            Try(new ObjectId(id)) match {
              case Success(boid) => Right(boid)
              case Failure(_)    => Left(s"ID $id was invalid")
            }
        }

      def unbind(key: String, value: ObjectId): String = value.toString
    }
}
