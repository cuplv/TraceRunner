package edu.colorado

import java.util.concurrent.atomic.AtomicInteger

import edu.colorad.cs.TraceRunner.Config
import soot._
import soot.jimple.Jimple

import scala.collection.JavaConversions._
import scala.util.matching.Regex

/**
 * Created by s on 9/30/16.
 */
object Utils {
  def globToRegex(glob: String): String = {
    glob.flatMap((a: Char) =>
      a match {
        case '*' => ".*"
        case '?' => "."
        case '.' => "\\."
        case '\\' => "\\\\"
        case a => List(a)
      }
    )
  }
  def packageGlobToSignatureMatchingRegex(glob: String): String = {
    globToRegex(glob)
  }
  val nameIndex = new AtomicInteger(0);
  def nextName(label: String): String = {
    "traceRunnerTempVar_" + label + "_" + nameIndex.getAndIncrement()
  }
  def autoBox(v: Value): Value = {
    v.getType() match {
      case i: IntType => {
        val methods =
          Scene.v().getSootClass("java.lang.Integer").getMethods()

        val boxmethod = Scene.v()
          .getSootClass("java.lang.Integer")
          .getMethod("java.lang.Integer valueOf(int)")
          Jimple.v ().newStaticInvokeExpr (boxmethod.makeRef (), List[Value] (v))
      }
      case i: BooleanType => {
        ???
      }
      case i: FloatType => {
        ???
      }
      case i: LongType => {
        ???
      }
      case i: RefType => v
      case _ => ???
    }

  }


}
