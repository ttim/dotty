package dotty.tools
package dotc

import core._
import Contexts._, Periods._, Symbols._
import io.PlainFile
import util.{SourceFile, NoSource}

class Run(comp: Compiler)(implicit ctx: Context) {

  var units: List[CompilationUnit] = _

  def getSource(fileName: String): SourceFile = {
    val f = new PlainFile(fileName)
    if (f.exists) new SourceFile(f)
    else {
      ctx.error(s"not found: $fileName")
      NoSource
    }
  }

  def compile(fileNames: List[String]): Unit = {
    val sources = fileNames map getSource
    if (sources forall (_.exists)) {
      units = sources map (new CompilationUnit(_))
      for (phase <- ctx.allPhases)
        phase.runOn(units)
    }
  }
}