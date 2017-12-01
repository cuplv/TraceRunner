package edu.colorado.plv.libtrace.specs

/**
  * @author Shawn Meier
  *         Created on 11/27/17.
  * @note This is the original spec language as used in the callback-verification dynamic verifier
  */
object SpecAST {
  sealed abstract class EnDiRule //Enable disable rule
  case class EnableAllow    (matcher : Matcher, effect : Message) extends EnDiRule
  case class DisableDisallow(matcher : Matcher, effect : Message) extends EnDiRule

  /**
    * Rule with implication (regexp matches for enable does not match for disable)
    * @param matcher Regular expression which must match entire previous string for effect to work
    * @param effect Message to be enabled/disabled or allowed/disallowed
    */
  sealed abstract class ImplRule(matcher : Matcher, effect : Message)

  sealed abstract class Matcher //parameterized regular expression
  case class False() extends Matcher

  case class Union(m1 : Matcher, m2 : Matcher) extends Matcher
  case class Intersection(m1 : Matcher, m2 : Matcher) extends Matcher
  case class Negation(m : Matcher) extends Matcher //Commented out in favor of using a negation of single message
  case class KleeneStar(m: Matcher) extends Matcher //Commented out since the only two uses of this are
  case class Seq(m1 : Matcher, m2 : Matcher) extends Matcher

  case class AnyMessage() extends Matcher

  sealed abstract class Message extends Matcher


  case class CallbackEntry(id: MethodIdentifier, reciever : Value, params : List[Value]) extends Message
  case class CallinExit   (id: MethodIdentifier, receiver : Value, params : List[Value], returnValue : Value) extends Message

  case class CallbackExit(id : MethodIdentifier, receiver : Value, params : List[Value], returnValue : Value) extends Message
  case class CallinEntry (id : MethodIdentifier, receiver : Value, params : List[Value]) extends Message

  /**
    * A way of matching a method, callback or callin
    * @param methodName the fully qualified method name (e.g. android.app.Activity.onCreate)
    * @param paramTypes the fully qualified type of each parameter
    * @param returnType the fully qualified return type
    */
  case class MethodIdentifier(returnType : String, methodName: String, paramTypes : List[String])

  sealed abstract class Value
  case class Var(name : String) extends Value
  case class BoolConst(b : Boolean) extends Value
  case class NullConst() extends Value
  case class DontCare() extends Value
  //TODO: there are more but I am not sure we need them yet


  def prettyPrintMethodIdentifier(mid: MethodIdentifier, reciever : Value, params : List[Value]): String = {
    mid match {
      case MethodIdentifier(returnType, methodName, paramTypes) => {
        assert(paramTypes.length == params.length)
        val paramString = (params zip paramTypes).foldLeft("")((acc: String, v: (Value,String)) =>{
          val next = s"${prettyPrint(v._1)} : ${v._2}"
          if(acc=="") next else s"${acc}, $next"
        })
        s"[${prettyPrint(reciever)}] $returnType ${methodName}(${paramString})"
      }
    }
  }
  def prettyPrint(matcher: Matcher): String = matcher match {
    case CallbackEntry(methodIdentifier, reciever, params) =>
      s"[CB] [ENTRY] ${prettyPrintMethodIdentifier(methodIdentifier, reciever, params)}"
    case CallbackExit(methodIdentifier, receiver, params, returnValue) =>
      s"${prettyPrint(returnValue)} = [CB] [EXIT] ${prettyPrintMethodIdentifier(methodIdentifier, receiver, params)}"
    case CallinEntry(methodIdentifier, receiver, params) =>
      s"[CI] [ENTRY] ${prettyPrintMethodIdentifier(methodIdentifier, receiver, params)}"
    case CallinExit(methodIdentifier, receiver, params, returnValue) =>
      s"${prettyPrint(returnValue)} = [CI] [EXIT] ${prettyPrintMethodIdentifier(methodIdentifier, receiver, params)}"
    case KleeneStar(matcher2) => s"${prettyPrint(matcher2)}[*]"
    case False() => "FALSE"
    case AnyMessage() => "TRUE"
    case Seq(m1, m2) => s"(${prettyPrint(m1)});(${prettyPrint(m2)})"
    case Union(m1, m2) => s"(${prettyPrint(m1)}) | (${prettyPrint(m2)})"
    case Intersection(m1,m2) => s"(${prettyPrint(m1)}) & (${prettyPrint(m2)})"
    case Negation(m) => s"!(${prettyPrint(m)})"
  }

  def prettyPrint(reciever: Value): String = reciever match{
    case DontCare() => "#"
    case Var(v) => v
    case BoolConst(b) => if(b) "TRUE" else "FALSE"
    case NullConst() => "NULL"
  }

  def prettyPrint(rule : EnDiRule): String = rule match {
    case EnableAllow(matcher, effect) =>"SPEC " + prettyPrint(matcher) + " |+ " + prettyPrint(effect)
    case DisableDisallow(matcher, effect) => s"SPEC ${prettyPrint(matcher)} |- ${prettyPrint(effect)}"
  }
  def prettyPrint(rules: List[EnDiRule]): String = rules.foldLeft("")((acc, v: EnDiRule) => {
    val vp = prettyPrint(v)
    if(acc == "") vp else acc + ";\n" + vp
  })
}
