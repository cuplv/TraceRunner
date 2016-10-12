package edu.colorado

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.concurrent.locks.ReentrantLock

import edu.colorad.cs.TraceRunner.Config
import soot.jimple.internal.{InvokeExprBox, JVirtualInvokeExpr}
import soot.jimple.{AbstractStmtSwitch, InvokeStmt, Jimple, StringConstant}
import soot.util.Chain
import soot.{Body, BodyTransformer, Local, PatchingChain, RefType, Scene, SootClass, SootMethod, Value, VoidType}

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

class CallinInstrumenter(config: Config) extends BodyTransformer{
 //TODO: somehow run once code can be run multiple times, fix this

  val applicationPackages = config.applicationPackages.map((a:String) =>{
    Utils.packageGlobToSignatureMatchingRegex(a).r
  })



  val callinInstrumentClass: String = "edu.colorado.plv.tracerunner_runtime_instrumentation.TraceRunnerRuntimeInstrumentation"

  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]): Unit = {


    if(Synchronizer.runSetup.get()){ //nuclear option to run only once, TODO: fix this
      Synchronizer.lock.lock()
      if(Synchronizer.runSetup.get()) {
        Scene.v().getSootClass(callinInstrumentClass)
          .setApplicationClass()
        Scene.v().getSootClass("edu.colorado.plv.tracerunner_runtime_instrumentation.LogDat").setApplicationClass()
        Synchronizer.runSetup.set(false)
      }
      Synchronizer.lock.unlock()
    }



    val declaration: String = b.getMethod.getDeclaration
    val method: SootMethod = b.getMethod()
    val name: String = b.getMethod.getName
    //val signature: String = b.getMethod.getSignature
    val signature: String = b.getMethod.getDeclaringClass.getName

    val logCallin: SootMethod = Scene.v().getSootClass(callinInstrumentClass).getMethod("void logCallin(java.lang.String,java.lang.String)")

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
            val methodName: String = stmt.getInvokeExpr.getMethod().getName()
            if (!config.isApplicationPackage(name1)){
              val signature: Local = Jimple.v().newLocal(Utils.nextName("signaturename"), RefType.v("java.lang.String"))
              val methodname: Local = Jimple.v().newLocal(Utils.nextName("methodname"), RefType.v("java.lang.String"))
              //Create signature information
              val sigassign = Jimple.v().newAssignStmt(signature, StringConstant.v(name1))
              units.insertBefore(sigassign,i)
              units.insertBefore(Jimple.v().newAssignStmt(methodname, StringConstant.v(methodName)),i)
              //log that callin has happened
              val expr: Value = Jimple.v().newStaticInvokeExpr(logCallin.makeRef(), List[Local](signature,methodname).asJava)
              units.insertBefore(Jimple.v().newInvokeStmt(expr), i)
            }

          }
        })
      }
    }else {}
  }
}
