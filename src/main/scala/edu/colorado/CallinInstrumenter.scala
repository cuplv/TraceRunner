package edu.colorado

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.concurrent.locks.ReentrantLock

import edu.colorad.cs.TraceRunner.Config
import soot.jimple.{AbstractStmtSwitch, InvokeStmt}
import soot.util.Chain
import soot.{Body, BodyTransformer, PatchingChain, Scene, SootClass, SootMethod}

import scala.collection.JavaConversions._
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



  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]): Unit = {


    if(Synchronizer.runSetup.get()){ //nuclear option to run only once, TODO: fix this
      Synchronizer.lock.lock()
      if(Synchronizer.runSetup.get()) {
        val clazz: SootClass = Scene.v().getSootClass("edu.colorado.plv.tracerunner_runtime_instrumentation.TraceRunnerRuntimeInstrumentation")
        if (!clazz.isApplicationClass()) {
          //println("run from thread: " + Thread.currentThread().getId)
          clazz.setApplicationClass()
        }
        Synchronizer.runSetup.set(false)
      }
      Synchronizer.lock.unlock()
    }



    val declaration: String = b.getMethod.getDeclaration


    val method: SootMethod = b.getMethod()


    val name: String = b.getMethod.getName


    val signature: String = b.getMethod.getSignature

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
                      println(stmt) //TODO
          }
        })
      }
    }else {}
  }
}
