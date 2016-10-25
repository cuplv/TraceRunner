package edu.colorado

import java.util

import edu.colorad.cs.TraceRunner.Config
import soot.jimple._
import soot.{ArrayType, Body, BodyTransformer, Local, PatchingChain, RefType, Scene, SootMethod, Value}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

/**
  * Created by s on 10/24/16.
  */
class CallbackInstrumenter(config: Config, instrumentationClasses: scala.collection.mutable.Buffer[String]) extends BodyTransformer {
  val applicationPackages = config.applicationPackages.map((a:String) =>{
    Utils.packageGlobToSignatureMatchingRegex(a).r
  })
  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]) = {
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
      val paramCount: Int = method.getParameterCount

      val args = new scala.collection.mutable.ListBuffer[Local]()
      var lastArg: Option[soot.Unit] = None
      var thisRef: Option[soot.Unit] = None
      for (i: soot.Unit <- units.snapshotIterator()) {

        i.apply(new AbstractStmtSwitch {
          override def caseReturnStmt(stmt: ReturnStmt): Unit = {
            stmt.getOp match{
              case l: Local => {
                val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
                val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
                val logReturn: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INATRUMENTATION_CLASS)
                  .getMethod("void logCallbackReturn(java.lang.String,java.lang.String,java.lang.Object)")
                units.insertBefore(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),i)
                units.insertBefore(Jimple.v().newAssignStmt(methodname, StringConstant.v(method.getName)),i)
                val returnTmp = Jimple.v().newLocal(Utils.nextName("returnTmp"), RefType.v("java.lang.Object"))
                units.insertBefore(Jimple.v().newAssignStmt(returnTmp, Utils.autoBox(l)),i)

                units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logReturn.makeRef(),
                  List[Local](lsignature, methodname, returnTmp))),i)
              }
              case l: Constant => {
                val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
                val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
                val logReturn: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INATRUMENTATION_CLASS)
                  .getMethod("void logCallbackReturn(java.lang.String,java.lang.String,java.lang.Object)")
                units.insertBefore(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),i)
                units.insertBefore(Jimple.v().newAssignStmt(methodname, StringConstant.v(method.getName)),i)
                units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logReturn.makeRef(),
                  List[Value](lsignature, methodname, l))),i)
              }
              case l: Expr => {
                ???
              } //Probably no expr in return statement
              case _ => ???
            }

          }
          override def caseIdentityStmt(stmt: IdentityStmt) = {
            val rvalue: Value = stmt.getRightOpBox.getValue
            rvalue match {
              case r: ParameterRef => {
                println(stmt)
                stmt.getLeftOp match {
                  case l: Local => args += l
                  case _ => ???
                }
                lastArg = Some(i)

              }
              case r: ThisRef => {
                thisRef = Some(i)
              }
              case _ => {}
            }
          }
        })
      }
      val lastArgU = lastArg match {
        case Some(l) => {

          //Create new local for args array
          val inputArgs: Local = Jimple.v().newLocal(Utils.nextName("inputArgs"), ArrayType.v(RefType.v("java.lang.Object"), 1))
          //add call to entry method
          val logCallback: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INATRUMENTATION_CLASS)
            .getMethod("void logCallbackEntry(java.lang.String,java.lang.String,java.lang.Object[])")
          val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
          val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
          val expr= Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logCallback.makeRef(), List[Local](lsignature, methodname, inputArgs)))
          units.insertAfter(expr,l)

          units.insertAfter(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),l)
          units.insertAfter(Jimple.v().newAssignStmt(methodname, StringConstant.v(method.getName)),l)


          //assing this ref to first element of inputArgs as receiver
          val newThisRef = b.getThisLocal
          val thisAssign = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(inputArgs, IntConstant.v(0)), newThisRef)
          units.insertAfter(thisAssign,l)

          val tuples: ListBuffer[(Local, Int)] = args zip (1 to args.length).toList
          tuples.foreach((a: (Local,Int)) => {
            val tmpLocal: Local = Jimple.v().newLocal(Utils.nextName("inputArgs"), RefType.v("java.lang.Object"))

            val argAssign: AssignStmt = Jimple.v().newAssignStmt(tmpLocal, Utils.autoBox(a._1))
            val argAssign2: AssignStmt = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(inputArgs, IntConstant.v(a._2)), tmpLocal)
            units.insertAfter(argAssign2,l)
            units.insertAfter(argAssign, l)
          })
          units.insertAfter(Jimple.v().newAssignStmt(inputArgs, Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"), IntConstant.v(args.length + 1))), l)
        }
        case None => {
          thisRef match {
            case Some(thisRef) => {

              val inputArgs: Local = Jimple.v().newLocal(Utils.nextName("inputArgs"), ArrayType.v(RefType.v("java.lang.Object"), 1))
              val newThisRef = b.getThisLocal
              val thisAssign = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(inputArgs, IntConstant.v(0)), newThisRef)
              val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
              val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
              val logCallback: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INATRUMENTATION_CLASS)
                .getMethod("void logCallbackEntry(java.lang.String,java.lang.String,java.lang.Object[])")
              val expr= Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logCallback.makeRef(), List[Local](lsignature, methodname, inputArgs)))

              units.insertAfter(expr,thisRef)
              units.insertAfter(thisAssign, thisRef)
              units.insertAfter(
                Jimple.v().newAssignStmt(inputArgs, Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"),
                  IntConstant.v(args.length + 1))),
                thisRef)
              //Add call to entry method
//              val inputArgs: Local = Jimple.v().newLocal(Utils.nextName("inputArgs"), ArrayType.v(RefType.v("java.lang.Object"), 1))



              units.insertAfter(Jimple.v().newAssignStmt(methodname, StringConstant.v(method.getName)),thisRef)
              units.insertAfter(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),thisRef)

            }
            case None => ??? //Shouldn't happen
          }
        }
      }



      //TODO: log exit in case of no return

    }



  }
}
