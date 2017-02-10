package edu.colorado

import java.util.concurrent.atomic.AtomicInteger

import edu.colorad.cs.TraceRunner.Config
import soot._
import soot.jimple.{Jimple, NullConstant}

import scala.collection.JavaConversions._
import scala.util.matching.Regex

/**
 * Created by s on 9/30/16.
 */
object Utils {
  def isFrameworkClass(getDeclaringClass: SootClass): Boolean = isFrameworkClass(getDeclaringClass.getName)

  val source = scala.io.Source.fromFile(TraceRunnerOptions.FRAMEWORK_FILTER_FILE)
  val filter_lines = (try source.mkString finally source.close()).split("\n")
  val framework_match_regex = filter_lines.map(globToRegex).map(a => a.r)
  def isFrameworkClass(clazz: String): Boolean ={
    framework_match_regex.exists((a: Regex) => clazz match{
      case a() => true
      case _ => false
    })
  }
  def semicolonSeparatedGlobsToRegex(list: String): Set[Regex] ={
    list.split(":").map(a => globToRegex(a).r).toSet
  }
  def sootClassMatches(clazz: SootClass, regexSet: Set[Regex]): Boolean ={
    val name: String = clazz.getName
    regexSet.exists(a => a.findFirstIn(name) match{
      case Some(_) => true
      case None => false
    })
  }
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
  def assertNotPhantom(c: SootClass) ={
    if(c.isPhantom){
      throw new RuntimeException("please check classpath, phantom java runtime detected")
    }
  }
  def packageGlobToSignatureMatchingRegex(glob: String): String = {
    globToRegex(glob)
  }
  val nameIndex = new AtomicInteger(0);
  def nextName(label: String): String = {
    "traceRunnerTempVar_" + label + "_" + nameIndex.getAndIncrement()
  }
  def autoBox(v: Value): Value = {
    val integerClazz: SootClass = Scene.v()
      .getSootClass("java.lang.Integer")
      assertNotPhantom(integerClazz)
    v.getType() match {
      case i: IntType => {
        val boxmethod = integerClazz
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
      case i: ArrayType =>v //TODO: test that this works (https://docs.oracle.com/javase/specs/jls/se7/html/jls-10.html)
      case i: BooleanType => {
        val boxmethod = Scene.v()
          .getSootClass("java.lang.Boolean")
          .getMethod("java.lang.Boolean valueOf(boolean)")
        Jimple.v ().newStaticInvokeExpr (boxmethod.makeRef (), List[Value] (v))
      }
      case i: NullType => {
        NullConstant.v()
      }
      case i: CharType =>{
        val boxmethod = Scene.v()
          .getSootClass("java.lang.Character")
          .getMethod("java.lang.Character valueOf(char)")
        Jimple.v().newStaticInvokeExpr(boxmethod.makeRef(), List[Value](v))
      }
      case i: ShortType => {
        val boxmethod = Scene.v()
          .getSootClass("java.lang.Short")
          .getMethod("java.lang.Short valueOf(short)")
        Jimple.v().newStaticInvokeExpr(boxmethod.makeRef(), List[Value](v))
      }
      case i: ByteType => {
        val boxmethod = Scene.v()
          .getSootClass("java.lang.Byte")
          .getMethod("java.lang.Byte valueOf(byte)")
        Jimple.v().newStaticInvokeExpr(boxmethod.makeRef(), List[Value](v))
      }
      case i => {
        ???
      } //Note no boolean type as these are ints
    }

  }


}
