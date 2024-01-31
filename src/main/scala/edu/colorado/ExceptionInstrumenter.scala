package edu.colorado

import java.util

import edu.colorado.TraceRunner.Config
import soot.jimple._
import soot.jimple.internal.JIdentityStmt
import soot.util.Chain
import soot.{Body, BodyTransformer, Local, PatchingChain, RefType, Scene, SootMethod, Trap, Unit, UnitBox, Value}

import scala.jdk.CollectionConverters._

/**
  * Created by s on 11/21/16.
  */
class ExceptionInstrumenter(config: Config, instrumentationClasses: scala.collection.mutable.Buffer[String]) extends BodyTransformer {
//TODO: log exceptions in constructos
  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]) = {

    val logException: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
      .getMethod("void logException(java.lang.Throwable,java.lang.String,java.lang.String)")
    val logCallbackException: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
      .getMethod("void logCallbackException(java.lang.Throwable,java.lang.String,java.lang.String)")
    val declaration: String = b.getMethod.getDeclaration
    val method: SootMethod = b.getMethod()

    val name: String = b.getMethod.getName
    val signature: String = b.getMethod.getDeclaringClass.getName
    val method_in_app: Boolean = Utils.sootClassMatches(b.getMethod.getDeclaringClass, config.applicationPackagesr.toSet) && !Utils.isFrameworkClass(signature)

    //TODO: no exception instrumentation on synchronized methods see issue #
    if(method_in_app && name != "<clinit>" && !method.isStatic && name != "<init>" && !method.isSynchronized) {

      // put an exception log in every existing catch block
      val units: PatchingChain[Unit] = b.getUnits
      val endstmt: Unit = units.getLast
      val traps: Chain[Trap] = b.getTraps
      traps.iterator().asScala.foreach((a: Trap) => {
        val handlerUnit: Unit = a.getHandlerUnit
        handlerUnit match{
          case h: JIdentityStmt => {
            logExceptionUnits(b, logException, units, h, false, logCallbackException)
          }
          case h =>{
            ??? //JVM and dalvik vm throw bytecode verification exception if this happens
          }
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
      val exnUnit: IdentityStmt = Jimple.v.newIdentityStmt(exceptionLocal, Jimple.v().newCaughtExceptionRef())
      val shutdownMethod: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
        .getMethod("void shutdownAndAwaitTermination()")
      //TODO: test await shutdown cmd
      units.addLast(exnUnit)

      b.getTraps.addLast(Jimple.v.newTrap(exceptionType.getSootClass,beginstmt,exnUnit,exnUnit))


      b.getLocals.addLast(exceptionLocal)
//      units.insertAfter(exnUnit,nop1)
      units.insertAfter(Jimple.v().newReturnVoidStmt(), exnUnit)
      val logStmt: Unit = logExceptionUnits(b, logException, units, exnUnit, true, logCallbackException)
      units.insertAfter(Jimple.v().newThrowStmt(exceptionLocal),logStmt)
      units.insertAfter(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(shutdownMethod.makeRef())), logStmt)

    }
  }

  def logExceptionUnits(b: Body, logException: SootMethod, units: PatchingChain[Unit], h: IdentityStmt, isTerminal: Boolean, logCallbackException: SootMethod) = {
    val lval = Jimple.v().newLocal(Utils.nextName("lval"), h.getLeftOp().getType)
    val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
    val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
    val argsLog: List[Local] = List[Local](lval, lsignature, methodname)
    val logCall = Jimple.v().newStaticInvokeExpr(logException.makeRef(), argsLog:_*)
    val logStmt: InvokeStmt = Jimple.v().newInvokeStmt(logCall)
    val emit: Unit = if(isTerminal) {
      val logTerm = Jimple.v().newStaticInvokeExpr(logCallbackException.makeRef(), argsLog:_*)
      val logTermStmt: InvokeStmt = Jimple.v().newInvokeStmt(logTerm)
      units.insertAfter(logTermStmt,h)
      logTermStmt
    }else{logStmt}
    units.insertAfter(logStmt, h)
    units.insertAfter(Jimple.v().newAssignStmt(methodname, StringConstant.v(b.getMethod.getName)), h)
    units.insertAfter(Jimple.v().newAssignStmt(lsignature, StringConstant.v(b.getMethod.getDeclaringClass.getName)), h)
    units.insertAfter(Jimple.v().newAssignStmt(lval, h.getLeftOp), h)

    emit

  }
}
