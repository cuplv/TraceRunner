package edu.colorado.plv.libtrace

import edu.colorado.plv.libtrace
import edu.colorado.plv.tracerunner_runtime_instrumentation.tracemsg.TraceMsgContainer.{CallbackEntryMsg, CallbackExceptionMsg, CallbackExitMsg, CallinEntryMsg, CallinExceptionMsg, CallinExitMsg, TraceMsg}

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

/**
  * @author Shawn Meier
  *         Created on 9/8/17.
  */
object TraceStructure {
  type RawTrace = List[TraceMsg]

  abstract case class CMsg()
  sealed abstract case class CCallbackExit()
  case class CCallbackExitException( exit: CallbackExceptionMsg) extends CCallbackExit
  case class CCallbackExitNormal( exit: CallbackExitMsg) extends CCallbackExit

  sealed abstract case class CCallinExit()
  case class CCallinExitException( exit : CallinExceptionMsg)
  case class CCallinExitNormal( exit : CallinExitMsg)


  case class CTrace(children : List[CCallback])
  case class CCallback(children : List[CCallin], entry : CallbackEntryMsg, exit: CCallbackExit) extends CMsg
  case class CCallin(children : List[CCallback], entry : CallinEntryMsg, exit : CCallinExit) extends CMsg
  def getTraceTree(trace: RawTrace)= {
    iGetTraceTree(trace, List(), CTrace(List()))
  }
  def isExitMsg(m: TraceMsg) = m.`type` match{
    case TraceMsg.MsgType.CALLBACK_EXIT => true
    case TraceMsg.MsgType.CALLIN_EXIT => true
    case TraceMsg.MsgType.CALLIN_EXEPION => true
    case TraceMsg.MsgType.CALLBACK_EXCEPTION => true
    case _ => false
  }
  def exitMsgMatch(entry : TraceMsg, exit : TraceMsg) = (entry.`type`) match{
    case (TraceMsg.MsgType.CALLIN_ENTRY) => {
      val cEntry = entry.getCallinEntry
      exit.`type` match{
        case TraceMsg.MsgType.CALLIN_EXIT => ???
        case TraceMsg.MsgType.CALLIN_EXEPION => ???
        case _ => false
      }
    }
    case (TraceMsg.MsgType.CALLBACK_ENTRY) => {
      val cEntry = entry.getCallinEntry
      exit.`type` match{
        case TraceMsg.MsgType.CALLBACK_EXIT => ???
        case TraceMsg.MsgType.CALLBACK_EXCEPTION => ???

      }
    }
    case _ => false

  }

  @tailrec
  def iGetTraceTree(trace: RawTrace, stack: List[TraceMsg], acc:CTrace): CTrace = trace match{
    case h::t if isExitMsg(h)=> stack match{
      case top::rest if exitMsgMatch(top, h) => ???
      case _ => acc //Corrupted trace truncate rest
    }
    case h::t => iGetTraceTree(t, h::stack, acc)
    case Nil => acc //End of trace return accumulated
  }




}
