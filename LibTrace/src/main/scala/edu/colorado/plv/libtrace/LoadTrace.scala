package edu.colorado.plv.libtrace

import java.io.{File, FileInputStream}
import java.util

import edu.colorado.plv.tracerunner_runtime_instrumentation.tracemsg.TraceMsgContainer
import edu.colorado.plv.tracerunner_runtime_instrumentation.tracemsg.TraceMsgContainer.TraceMsg

/**
  * @author Shawn Meier
  *         Created on 9/7/17.
  */
object LoadTrace {

  def loadTrace(path: String): Seq[TraceMsg] = loadTrace(new File(path))

  //TODO: this would be more efficient with ListBuffer, see "Programming in Scala p 466"
  def loadTrace(file: File): Seq[TraceMsg] = {
    val stream = new FileInputStream(file)

    val backwardTrace = iLoadTrace(stream, Nil)

    backwardTrace.reverse.map(a => a.msg)
  }

  private def iLoadTrace(stream: FileInputStream, listSoFar: List[TraceMsgContainer]): List[TraceMsgContainer] = {
    val parsedMsg: Option[TraceMsgContainer] = TraceMsgContainer.parseDelimitedFrom(stream)
    parsedMsg match {
      case Some(traceMsg : TraceMsgContainer) => iLoadTrace(stream, traceMsg :: listSoFar)
      case None => listSoFar
    }
  }
}
