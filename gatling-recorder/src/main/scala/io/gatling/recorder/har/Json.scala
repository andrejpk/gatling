/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.har

import java.io.InputStream
import java.util.{ List => JList, Map => JMap }

import scala.language.dynamics

import com.fasterxml.jackson.databind.ObjectMapper

private[har] object Json {

  val objectMapper = new ObjectMapper

  def parseJson(is: InputStream) = new Json(objectMapper.readValue(is, classOf[Object]))

  implicit def jsonToString(s: Json): String = s.toString
  implicit def jsonToInt(s: Json): Int = s.toInt
}

private[har] class JsonException(desc: String) extends Exception(desc)

private[har] class JsonIterator(i: java.util.Iterator[Object]) extends Iterator[Json] {
  override def hasNext: Boolean = i.hasNext
  override def next: Json = new Json(i.next)
}

private[har] class Json(val o: Object) extends Seq[Json] with Dynamic {

  override def toString: String = if (o == null) "null" else o.toString

  def toOption: Option[Json] = if (o == null) None else Some(this)

  def toInt: Int = o match {
    case i: java.lang.Integer => i
    case _                    => throw new JsonException(s"Object $o isn't an integer")
  }

  def toLong: Long = o match {
    case i: java.lang.Integer => Int.int2long(i)
    case l: java.lang.Long    => l
    case d: java.lang.Double  => d.longValue
    case f: java.lang.Float   => Float.float2double(f).longValue()
    case _                    => throw new JsonException(s"Object $o isn't a long")
  }

  def toDouble: Double = o match {
    case i: java.lang.Integer => Int.int2double(i)
    case l: java.lang.Long    => Long.long2double(l)
    case d: java.lang.Double  => d
    case f: java.lang.Float   => Float.float2double(f)
    case _                    => throw new JsonException(s"Object $o isn't a floating point number")
  }

  def apply(key: String): Json = o match {
    case m: JMap[_, _] => new Json(m.get(key).asInstanceOf[Object])
    case _             => throw new JsonException(s"Object $o isn't a map thus, can't select key=$key")
  }

  def apply(idx: Int): Json = o match {
    case a: JList[_] => new Json(a.get(idx).asInstanceOf[Object])
    case _           => throw new JsonException(s"Object $o isn't a list")
  }

  def length: Int = o match {
    case a: JList[_]   => a.size
    case m: JMap[_, _] => m.size
    case _             => throw new JsonException(s"Object $o isn't a list or map")
  }

  def iterator: Iterator[Json] = o match {
    case a: JList[_] => new JsonIterator(a.asInstanceOf[JList[Object]].iterator)
    case _           => Iterator.empty
  }

  def selectDynamic(name: String): Json = apply(name)

  def applyDynamic(name: String)(arg: Any): Json = {
    arg match {
      case s: String => apply(name)(s)
      case n: Int    => apply(name)(n)
      case u: Unit   => apply(name)
    }
  }
}
