package dotty.tools.dottydoc
package model

import comment._
import dotty.tools.dotc
import dotc.core.Types._
import dotc.core.Contexts.Context
import dotc.core.Symbols.Symbol
import dotc.core.{ Flags => DottyFlags }
import dotc.ast.Trees._


object factories {
  import dotty.tools.dotc.ast.tpd._
  import DottyFlags._

  type TypeTree = dotty.tools.dotc.ast.Trees.Tree[Type]

  def flags(t: Tree)(implicit ctx: Context): List[String] =
    (t.symbol.flags & SourceModifierFlags)
      .flagStrings.toList
      .filter(_ != "<trait>")
      .filter(_ != "interface")

  def path(t: Tree)(implicit ctx: Context): List[String] = {
    def pathList(tpe: Type): List[String] = tpe match {
      case t: ThisType =>
        pathList(t.tref)
      case t: NamedType if t.prefix == NoPrefix  && t.name.toString == "<root>" =>
        Nil
      case t: NamedType if t.prefix == NoPrefix =>
        t.name.toString :: Nil
      case t: NamedType =>
        pathList(t.prefix) :+ t.name.toString
    }

    val ref =
      if (t.symbol.isTerm) t.symbol.termRef
      else t.symbol.typeRef

    pathList(ref)
  }

  def returnType(t: Tree, tpt: TypeTree)(implicit ctx: Context): MaterializableLink = {
    def cleanTitle(title: String): String = title match {
      case x if x matches "[^\\[]+\\.this\\..+" => x.split("\\.").last
      case x if x matches "[^\\[]+\\[[^\\]]+\\]" =>
        val Array(tpe, params) = x.dropRight(1).split("\\[")
        s"""$tpe[${params.split(",").map(x => cleanTitle(x.trim)).mkString(", ")}]"""
      case _ => title
    }

    def cleanQuery(query: String): String = query match {
      case x if x matches "[^\\[]+\\[[^\\]]+\\]" => x.takeWhile(_ != '[')
      case _ => query
    }

    UnsetLink(Text(cleanTitle(tpt.show)), cleanQuery(tpt.show))
  }

  def typeParams(t: Tree)(implicit ctx: Context): List[String] = t match {
    case t: DefDef =>
      def variance(s: Symbol) =
        if (s is Covariant) "+"
        else if (s is Contravariant) "-"
        else ""
      t.tparams.map(p => variance(p.symbol) + p.show)
    case t: TypeDef if t.rhs.isInstanceOf[Template] =>
      // Get the names from the constructor method `DefDef`
      typeParams(t.rhs.asInstanceOf[Template].constr)
  }

  def paramLists(t: DefDef)(implicit ctx: Context): List[List[(String, MaterializableLink)]] = {
    def getParams(xs: List[ValDef]): List[(String, MaterializableLink)] = xs map { vd =>
      (vd.name.toString, UnsetLink(Text(vd.tpt.show), vd.tpt.show))
    }

    t.vparamss.map(getParams)
  }

  def filteredName(str: String) = str
    .replaceAll("\\$colon", ":")
    .replaceAll("\\$plus", "+")
    .replaceAll("\\$less", "<")
    .replaceAll("\\$eq", "=")
}
