/* Copyright (C) 2009 Thomas Rampelberg <pyronicide@gmail.com>

 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

/* Bencoding tools. The parser implements a full bencode grammar (string,
 * integer, list, dictionary).
 */
// XXX - NEED UNIT TESTS!!!
package org.saunter.bencode

import scala.collection.immutable._
import scala.util.parsing.combinator._
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator.lexical._

object Bencode extends StdTokenParsers {
  type Tokens = Lexer
  val lexical = new Tokens

  /**
   * Parse the given bencoded string and return the elements. This can be any
   * of the bencode types (string, int, list, hashmap).
   *
   * @param input the given bencoded string.
   * @return      an optional string, int, list or hashmap
   */
  // XXX - Probably should be doing some kind of logging here.
  def parse(input: String): Option[Any] =
    phrase(doc)(new lexical.Scanner(input)) match {
      case Success(result, _) => Some(result)
      case Failure(msg, _) => println(msg); None
      case Error(msg, _) => println(msg); None
    }

  /**
   * Definition of the grammar for a bencoded string. 'l', 'd', 'e' are all
   * keywoards that get passed up from the lexer. The string and int keywords
   * (':' and 'i') are not keywords that end up getting seen by the parser
   * since they are quietly consumed by the lexer.
   */
  def doc: Parser[Any] = ( num | str | list | dict )
  def num = numericLit ^^ { case x => x.toInt }
  def str = ident ^^ { case x => x }
  def list = "l" ~> rep1(doc) <~ "e" ^^ { case x => x }
  def dict = "d" ~> rep1(str ~ doc) <~ "e" ^^ {
    case x => HashMap(x map { case x ~ y => (x, y) }: _*) }

  /**
   * Generate a bencoded string from scala objects. This can handle the
   * entire bencoding grammar which means that Int, String, List and Map can be
   * encoded.
   */
  def encode(input: Any): String =
    input match {
      case x: Int => int(x)
      case x: String => string(x)
      case x: List[_] => list(x)
      case x: Map[String, _] => dictionary(x)
      case _ => ""
    }

  def int(input: Int): String =
    "i" + input + "e"

  def string(input: String): String =
    input.length + ":" + input

  def list(input: List[_]): String =
    "l" + input.map( x => encode(x)).mkString + "e"

  def dictionary(input: Map[String, _]): String =
    "d" + input.map(
      x => (x._1, encode(x._2))).flatMap( x => x._1 + x._2 ).mkString + "e"
}

