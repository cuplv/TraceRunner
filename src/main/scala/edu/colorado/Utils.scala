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
        val boxmethod = Scene.v()
          .getSootClass("java.lang.Integer")
          .getMethod("java.lang.Integer valueOf(int)")
          Jimple.v ().newStaticInvokeExpr (boxmethod.makeRef (), List[Value] (v))
      }
      case i: FloatType => {
        val boxmethod = Scene.v()
          .getSootClass("java.lang.Float")
          .getMethod("java.lang.Float valueOf(float)")
        Jimple.v ().newStaticInvokeExpr (boxmethod.makeRef (), List[Value] (v))

      }
      case i: LongType => {
        val boxmethod = Scene.v()
          .getSootClass("java.lang.Long")
          .getMethod("java.lang.Long valueOf(long)")
        Jimple.v ().newStaticInvokeExpr (boxmethod.makeRef (), List[Value] (v))
      }
      case i: DoubleType => {
        val boxmethod = Scene.v()
          .getSootClass("java.lang.Double")
          .getMethod("java.lang.Double valueOf(double)")
        Jimple.v ().newStaticInvokeExpr (boxmethod.makeRef (), List[Value] (v))
      }
      case i: RefType => v
      case _ => {
        ???
      } //Note no boolean type as these are ints
    }

  }


}
