package edu.colorado

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.concurrent.locks.ReentrantLock

import edu.colorad.cs.TraceRunner.Config
import soot.dava.internal.javaRep.DNewInvokeExpr
import soot.grimp.internal.{GDynamicInvokeExpr, GNewInvokeExpr, GStaticInvokeExpr}
import soot.jimple.internal._
import soot.jimple._
import soot.util.Chain
import soot.{ArrayType, Body, BodyTransformer, IntType, Local, PatchingChain, RefType, Scene, SootClass, SootMethod, Value, VoidType}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.matching.Regex

/**
 * Created by s on 9/21/16.
 */
object Synchronizer{
  var runSetup = new AtomicBoolean(true)
  var lock = new ReentrantLock()
}

class CallinInstrumenter(config: Config, instrumentationClasses: scala.collection.mutable.Buffer[String]) extends BodyTransformer{
 //TODO: somehow run once code can be run multiple times, fix this

  val applicationPackages = config.applicationPackages.map((a:String) =>{
    Utils.packageGlobToSignatureMatchingRegex(a).r
  })



  val callinInstrumentClass: String = "edu.colorado.plv.tracerunner_runtime_instrumentation.TraceRunnerRuntimeInstrumentation"

  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]): Unit = {


    if(Synchronizer.runSetup.get()){ //nuclear option to run only once, TODO: fix this
      Synchronizer.lock.lock()
      if(Synchronizer.runSetup.get()) {
          instrumentationClasses.map(a => Scene.v().getSootClass(a).setApplicationClass())
//        Scene.v().getSootClass(callinInstrumentClass)
//          .setApplicationClass()
//        Scene.v().getSootClass("edu.colorado.plv.tracerunner_runtime_instrumentation.TraceRunnerRuntimeInstrumentation$1").setApplicationClass()
//        Scene.v().getSootClass("edu.colorado.plv.tracerunner_runtime_instrumentation.LogDat").setApplicationClass()
        Synchronizer.runSetup.set(false)
      }
      Synchronizer.lock.unlock()
    }



    val declaration: String = b.getMethod.getDeclaration
    val method: SootMethod = b.getMethod()




    val name: String = b.getMethod.getName
    //val signature: String = b.getMethod.getSignature
    val signature: String = b.getMethod.getDeclaringClass.getName

    val logCallin: SootMethod = Scene.v().getSootClass(callinInstrumentClass).getMethod("void logCallin(java.lang.String,java.lang.String,java.lang.Object[],java.lang.Object)")

    val matches: Boolean = applicationPackages.exists((r: Regex )=>{
      signature match{
        case r() => true
        case _ => false
      }
    })
    if(matches) {
      val units: PatchingChain[soot.Unit] = b.getUnits;
      for (i: soot.Unit <- units.snapshotIterator()) {
        i.apply(new AbstractStmtSwitch {
          override def caseInvokeStmt(stmt: InvokeStmt) = {
            val name1: String = stmt.getInvokeExpr.getMethod.getDeclaringClass.getName
            val currentMethod: SootMethod = stmt.getInvokeExpr.getMethod()
            val methodName: String = currentMethod.getName()
            if (!config.isApplicationPackage(name1)){
              

              //Arguments
              val argCount: Int = stmt.getInvokeExpr.getArgCount
              val arguments: Local = Jimple.v().newLocal(Utils.nextName("arguments"), ArrayType.v(RefType.v("java.lang.Object"), 1))
              units.insertBefore(Jimple.v().newAssignStmt(
                arguments, Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"), IntConstant.v(argCount + 1))), i)

              val receiver = stmt.getInvokeExpr match {
                case i: InstanceInvokeExpr => i.getBase
                case i: StaticInvokeExpr => NullConstant.v() //TODO: null value here
                case _ => ???
              }
              units.insertBefore(Jimple.v().newAssignStmt(Jimple.v().newArrayRef(arguments, IntConstant.v(0)), receiver), i)

              (1 until (argCount+1)).foreach( a => {
                val arg: Value = stmt.getInvokeExpr().getArg(a - 1)
                val tmpLocal = Jimple.v().newLocal(Utils.nextName("arguments"), RefType.v("java.lang.Object"))
                val argAssign: AssignStmt = Jimple.v().newAssignStmt(
                  tmpLocal , Utils.autoBox(arg))
                val argAssign2: AssignStmt = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(arguments, IntConstant.v(a)), tmpLocal)

                units.insertBefore(argAssign, i)
                units.insertBefore(argAssign2,i)
              })

              //Caller
              val newThisRef = b.getThisLocal
              val callerref: Local = Jimple.v().newLocal(Utils.nextName("callerref"), RefType.v("java.lang.Object"))
              units.insertBefore(Jimple.v().newAssignStmt(callerref, newThisRef),i)

              //Signature and method name
              val signature: Local = Jimple.v().newLocal(Utils.nextName("signaturename"), RefType.v("java.lang.String"))
              val methodname: Local = Jimple.v().newLocal(Utils.nextName("methodname"), RefType.v("java.lang.String"))
              //Create signature information
              val sigassign = Jimple.v().newAssignStmt(signature, StringConstant.v(name1))
              units.insertBefore(sigassign,i)

              //TODO: Capture return value
              val methodSignature = method.getSignature
              units.insertBefore(Jimple.v().newAssignStmt(methodname, StringConstant.v(methodName + methodSignature)),i)
              val expr: Value = Jimple.v().newStaticInvokeExpr(logCallin.makeRef(), List[Local](signature,methodname, arguments, callerref).asJava)
              units.insertBefore(Jimple.v().newInvokeStmt(expr), i)
            }

          }
        })
      }
    }else {}
  }
}
