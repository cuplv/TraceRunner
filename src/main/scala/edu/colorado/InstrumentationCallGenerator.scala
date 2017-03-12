package edu.colorado

import soot.Value

/**
  * Created by s on 3/12/17.
  */
class InstrumentationCallGenerator {
  sealed abstract case class InstParameter()

  sealed abstract case class InstConstant() extends InstParameter
  case class InstStringConstant(s: String) extends InstConstant
  case class InstIntegerConstant(i: Int) extends  InstConstant

  sealed abstract case class InstVariable() extends InstParameter
  case class WrapInstVariable(v : Value) extends InstParameter
  case class NormalInstVariable(v: Value) extends InstParameter

  case class ArrayParameter(l: List[InstParameter]) extends InstParameter


}
