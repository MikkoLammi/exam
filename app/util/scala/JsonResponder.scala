/*
 * Copyright (c) 2017 Exam Consortium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed
 * on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package util.scala

import io.ebean.Model
import play.api.mvc.{InjectedController, Result}
import play.libs.{Json => JavaJson}

import scala.language.implicitConversions


trait JsonResponder {
  self: InjectedController =>

  implicit def coursesList2Response[T <: Model](c: java.util.List[T]): Result = java2Response(c)

  implicit def course2Response[T <: Model](c: T): Result = java2Response(c)

  def java2Response[T <: Model](models: java.util.List[T]) =
    Ok(JavaJson.toJson(models).toString)

  def java2Response[T <: Model](models: java.util.Set[T]) =
    Ok(JavaJson.toJson(models).toString)

  def java2Response[T <: Model](model: T) =
    Ok(JavaJson.toJson(model).toString)

}
