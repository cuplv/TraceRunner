package edu.colorado

import java.util

import edu.colorad.cs.TraceRunner.Config
import soot.jimple.{GotoStmt, Jimple, NopStmt, StringConstant}
import soot.jimple.internal.JIdentityStmt
import soot.util.Chain
import soot.{Body, BodyTransformer, Local, PatchingChain, RefType, Scene, SootMethod, Trap, Unit, UnitBox, Value}

import scala.collection.JavaConversions._

/**
  * Created by s on 11/21/16.
  */
class ExceptionInstrumenter(config: Config, instrumentationClasses: scala.collection.mutable.Buffer[String]) extends BodyTransformer {

  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]) = {

    val logException: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
      .getMethod("void logException(java.lang.Throwable,java.lang.String,java.lang.String)")
    val declaration: String = b.getMethod.getDeclaration
    val method: SootMethod = b.getMethod()

    val name: String = b.getMethod.getName
    val signature: String = b.getMethod.getDeclaringClass.getName
    val method_in_app: Boolean = !Utils.isFrameworkClass(signature)
    if(method_in_app && name != "<clinit>" && !method.isStatic) {

      // put an exception log in every existing catch block
      val units: PatchingChain[Unit] = b.getUnits
      val endstmt: Unit = units.getLast
      val traps: Chain[Trap] = b.getTraps
      traps.iterator().foreach((a: Trap) => {
        val handlerUnit: Unit = a.getHandlerUnit
        handlerUnit match{
          case h: JIdentityStmt => {
            val lval = Jimple.v().newLocal(Utils.nextName("lval"), h.getLeftOp().getType)
            val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
            val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
            val logCall = Jimple.v().newStaticInvokeExpr(logException.makeRef(), List[Local](lval, lsignature, methodname))
            units.insertAfter(Jimple.v().newInvokeStmt(logCall),h)
            units.insertAfter(Jimple.v().newAssignStmt(methodname, StringConstant.v(name)),h)
            units.insertAfter(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),h)

            units.insertAfter(Jimple.v().newAssignStmt(lval, h.getLeftOp),h)
          }
          case h =>{
            ???
          } //Is it ever the case that identity stmt doesn't come after catch? TODO: test empty catch block
        }
      })

      //***put a catch block around full method to log and rethrow***
//      val nop1: NopStmt = Jimple.v().newNopStmt() //insert exn handling after this nop
//      units.addLast(nop1)
//      val endingNop: NopStmt = Jimple.v().newNopStmt()
//      units.addLast(endingNop)
//      val gotoNop: GotoStmt = Jimple.v().newGotoStmt(endingNop)
//      units.insertBefore(gotoNop,nop1)
      //Trap that sends exceptions from method body to new end block

      val beginstmt: Unit = units.getFirst
      val exceptionType: RefType = RefType.v("java.lang.Throwable")

      val exceptionLocal: Local = Jimple.v.newLocal(Utils.nextName("exception"), exceptionType)
      val exnUnit = Jimple.v.newIdentityStmt(exceptionLocal, Jimple.v().newCaughtExceptionRef())
      b.getTraps.addLast(Jimple.v.newTrap(exceptionType.getSootClass,beginstmt,exnUnit,exnUnit))


      b.getLocals.addLast(exceptionLocal)
//      units.insertAfter(exnUnit,nop1)
      units.addLast(exnUnit)
    }
  }
}
