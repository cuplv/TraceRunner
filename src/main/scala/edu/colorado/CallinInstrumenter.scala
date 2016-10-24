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
import soot.{ArrayType, Body, BodyTransformer, CharType, FloatType, IntType, Local, LongType, PatchingChain, RefType, Scene, SootClass, SootMethod, Type, Unit, Value, ValueBox, VoidType}

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

  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]){


    if(Synchronizer.runSetup.get()){ //nuclear option to run only once, TODO: fix this
      Synchronizer.lock.lock()
      if(Synchronizer.runSetup.get()) {
//          instrumentationClasses.map(a => Scene.v().getSootClass(a).setApplicationClass()) //TODO: commented out while trying to manually add dex
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



    val matches: Boolean = applicationPackages.exists((r: Regex )=>{
      signature match{
        case r() => true
        case _ => false
      }
    })
    if(matches && name != "<clinit>") {
      val units: PatchingChain[soot.Unit] = b.getUnits;
      for (i: soot.Unit <- units.snapshotIterator()) {
        i.apply(new AbstractStmtSwitch {

          override def caseAssignStmt(stmt: AssignStmt) = {
            val left: Value = stmt.getLeftOp
            val right: Value = stmt.getRightOp
            right match{
              case e: InvokeExpr => {
                instrumentInvokeExpr(b,i, e, None) //TODO: caputre output



//                units.insertAfter()
              }
              case _ => ()
            }
          }
          override def caseInvokeStmt(stmt: InvokeStmt) = {
            val invokeExpr: InvokeExpr = stmt.getInvokeExpr
            val returnType: Type = invokeExpr.getMethod.getReturnType
            returnType match{
              case r: VoidType => instrumentInvokeExpr(b, i, invokeExpr, None)
              case r: IntType => {
                val returnValue = Jimple.v().newLocal(Utils.nextName("returnValue"), IntType.v())
                units.insertBefore(Jimple.v().newAssignStmt(returnValue, invokeExpr),i)
                val boxedReturnValue = Jimple.v().newLocal(Utils.nextName("boxedReturnValue"), RefType.v("java.lang.Object"))
                units.insertBefore(Jimple.v().newAssignStmt(boxedReturnValue, Utils.autoBox(returnValue)),i)
                instrumentInvokeExpr(b,i,invokeExpr, Some(boxedReturnValue))
                units.remove(i)
              }
              case r: FloatType => {
                val returnValue = Jimple.v().newLocal(Utils.nextName("returnValue"), FloatType.v())
                units.insertBefore(Jimple.v().newAssignStmt(returnValue, invokeExpr),i)
                val boxedReturnValue = Jimple.v().newLocal(Utils.nextName("boxedReturnValue"), RefType.v("java.lang.Object"))
                units.insertBefore(Jimple.v().newAssignStmt(boxedReturnValue, Utils.autoBox(returnValue)),i)
                instrumentInvokeExpr(b,i,invokeExpr, Some(boxedReturnValue))
              }
              case r: LongType => {
                val returnValue = Jimple.v().newLocal(Utils.nextName("returnValue"), LongType.v())
                units.insertBefore(Jimple.v().newAssignStmt(returnValue, invokeExpr),i)
                val boxedReturnValue = Jimple.v().newLocal(Utils.nextName("boxedReturnValue"), RefType.v("java.lang.Object"))
                units.insertBefore(Jimple.v().newAssignStmt(boxedReturnValue, Utils.autoBox(returnValue)),i)
                instrumentInvokeExpr(b,i,invokeExpr, Some(boxedReturnValue))

              }
              case r: CharType => {
                val returnValue = Jimple.v().newLocal(Utils.nextName("returnValue"), CharType.v())
                units.insertBefore(Jimple.v().newAssignStmt(returnValue, invokeExpr),i)
                val boxedReturnValue = Jimple.v().newLocal(Utils.nextName("boxedReturnValue"), RefType.v("java.lang.Object"))
                units.insertBefore(Jimple.v().newAssignStmt(boxedReturnValue, Utils.autoBox(returnValue)),i)
                instrumentInvokeExpr(b,i,invokeExpr, Some(boxedReturnValue))
              }
              case r: ArrayType => {
                val returnValue = Jimple.v().newLocal(Utils.nextName("returnValue"), ArrayType.v(r.baseType, r.numDimensions))
                units.insertBefore(Jimple.v().newAssignStmt(returnValue, invokeExpr),i)
                val boxedReturnValue = Jimple.v().newLocal(Utils.nextName("boxedReturnValue"), RefType.v("java.lang.Object"))
                units.insertBefore(Jimple.v().newAssignStmt(boxedReturnValue, Utils.autoBox(returnValue)),i)
                instrumentInvokeExpr(b,i,invokeExpr, Some(boxedReturnValue))
              }
              case r: RefType => {
                val returnValue = Jimple.v().newLocal(Utils.nextName("returnValue"), RefType.v("java.lang.Object"))
                units.insertBefore(Jimple.v().newAssignStmt(returnValue, invokeExpr),i)
                instrumentInvokeExpr(b,i,invokeExpr, Some(returnValue))
                units.remove(i)
              }
              case r => {
                //Capture return value in local
                //TODO: finish me
                ???
              }
            }



            //TODO: Capture return value and put in 4th arg place

          }
        })
      }
    }

  }

  /**
    * if invokeExpr is calling something in the framework then put instrumentation around it
    * @param b
    * @param i
    * @param invokeExpr
    */
  def instrumentInvokeExpr(b: Body, i: Unit, invokeExpr: InvokeExpr, returnLocation: Option[Local]) = {
    val method = b.getMethod
    val units: PatchingChain[soot.Unit] = b.getUnits
    val logCallin: SootMethod = Scene.v().getSootClass(callinInstrumentClass).getMethod("void logCallin(java.lang.String,java.lang.String,java.lang.Object[],java.lang.Object)")
    val name1: String = invokeExpr.getMethod.getDeclaringClass.getName
    val currentMethod: SootMethod = invokeExpr.getMethod()
    val methodName: String = currentMethod.getName()
    val instrumentCallin: Boolean = !config.isApplicationPackage(name1)

    if (instrumentCallin) {


      //Arguments
      val argCount: Int = invokeExpr.getArgCount
      val arguments: Local = Jimple.v().newLocal(Utils.nextName("arguments"), ArrayType.v(RefType.v("java.lang.Object"), 1))
      units.insertBefore(Jimple.v().newAssignStmt(
        arguments, Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"), IntConstant.v(argCount + 1))), i)

      val receiver = invokeExpr match {
        case i: InstanceInvokeExpr => i.getBase
        case i: StaticInvokeExpr => NullConstant.v()
        case _ => ???
      }
      units.insertBefore(Jimple.v().newAssignStmt(Jimple.v().newArrayRef(arguments, IntConstant.v(0)), receiver), i)

      (1 until (argCount + 1)).foreach(a => {
        val arg: Value = invokeExpr.getArg(a - 1)
        val tmpLocal = Jimple.v().newLocal(Utils.nextName("arguments"), RefType.v("java.lang.Object"))
        val argAssign: AssignStmt = Jimple.v().newAssignStmt(
          tmpLocal, Utils.autoBox(arg))
        val argAssign2: AssignStmt = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(arguments, IntConstant.v(a)), tmpLocal)

        units.insertBefore(argAssign, i)
        units.insertBefore(argAssign2, i)
      })

      //Caller
      val newThisRef = b.getThisLocal
      val callerref: Local = Jimple.v().newLocal(Utils.nextName("callerref"), RefType.v("java.lang.Object"))
      units.insertBefore(Jimple.v().newAssignStmt(callerref, newThisRef), i)

      //Signature and method name
      val signature: Local = Jimple.v().newLocal(Utils.nextName("signaturename"), RefType.v("java.lang.String"))
      val methodname: Local = Jimple.v().newLocal(Utils.nextName("methodname"), RefType.v("java.lang.String"))
      //Create signature information
      val sigassign = Jimple.v().newAssignStmt(signature, StringConstant.v(name1))
      units.insertBefore(sigassign, i)

      val methodSignature = method.getSignature
      units.insertBefore(Jimple.v().newAssignStmt(methodname, StringConstant.v(methodName + methodSignature)), i)
      val expr: Value = Jimple.v().newStaticInvokeExpr(logCallin.makeRef(), List[Local](signature, methodname, arguments, callerref).asJava)
      units.insertBefore(Jimple.v().newInvokeStmt(expr), i)

      //TODO: locations of exits
      val callinExitMethodSig: String = "void logCallinExit(java.lang.String,java.lang.String,java.lang.Object,java.lang.String)"
      returnLocation match{
        case Some(returnLocation) => {
          //Log return value and exit
          val exitLogMethod: SootMethod = Scene.v().getSootClass(callinInstrumentClass).getMethod(callinExitMethodSig)
          val exitExpr = Jimple.v().newStaticInvokeExpr(exitLogMethod.makeRef(), List[Local](signature, methodname, returnLocation, signature).asJava)
          units.insertAfter(Jimple.v().newInvokeStmt(exitExpr), i)
        }
        case None =>{
          //Log exit and set return value to null (this is for void methods only)
          //Log return value and exit
          val exitLogMethod: SootMethod = Scene.v().getSootClass(callinInstrumentClass).getMethod(callinExitMethodSig)
          val nullreflocal = Jimple.v().newLocal(Utils.nextName("nullref"), RefType.v("java.lang.Object"))
          val exitExpr = Jimple.v().newStaticInvokeExpr(exitLogMethod.makeRef(), List[Local](signature, methodname, nullreflocal, signature).asJava)
          units.insertAfter(Jimple.v().newInvokeStmt(exitExpr), i)
          units.insertAfter(Jimple.v().newAssignStmt(nullreflocal, NullConstant.v()),i)
        }
      }
    }
  }
}
