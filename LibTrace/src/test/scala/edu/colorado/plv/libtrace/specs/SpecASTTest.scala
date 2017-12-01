package edu.colorado.plv.libtrace.specs

import edu.colorado.plv.libtrace.specs.SpecAST._
import org.scalatest.FunSuite

/**
  * @author Shawn Meier
  *         Created on 12/1/17.
  */
class SpecASTTest extends FunSuite {
  val onCreate = MethodIdentifier( "void", "android.app.Activity.onCreate", List("android.os.Bundle"))
  val onStart = MethodIdentifier("void", "android.app.Activity.onStart", List())

  val tests = List(
    (DisableDisallow(KleeneStar(False()), CallbackEntry(onCreate, Var("a"), List(DontCare()))),
      "SPEC FALSE[*] |- [CB] [ENTRY] [a] void android.app.Activity.onCreate(# : android.os.Bundle)"),
    (EnableAllow(Seq(KleeneStar(AnyMessage()), CallbackEntry(onCreate, Var("a"), List(DontCare()))), CallbackEntry(onStart, Var("a"), List())),
    "SPEC (TRUE[*]);([CB] [ENTRY] [a] void android.app.Activity.onCreate(# : android.os.Bundle)) |+ [CB] [ENTRY] [a] void android.app.Activity.onStart()")

  )
  val rule1 = DisableDisallow(KleeneStar(False()), CallbackEntry(onCreate, Var("a"), List(DontCare())))
  val stringRule1 = "SPEC FALSE[*] |- [CB] [ENTRY] [a] void android.app.Activity.onCreate(# : android.os.Bundle)"
  test("testPrettyPrint") {
//    assert(prettyPrint(rule1) == stringRule1)
    tests map (a => assert(prettyPrint(a._1) == a._2))
  }

}
