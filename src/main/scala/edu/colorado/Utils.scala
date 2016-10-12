package edu.colorado

import java.util.concurrent.atomic.AtomicInteger

import edu.colorad.cs.TraceRunner.Config

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


}
