package edu.colorado

import java.util
import edu.colorado.TraceRunner.Config
import soot.jimple._
import soot.{ArrayType, Body, BodyTransformer, Local, PatchingChain, RefType, Scene, SootMethod, SootMethodRef, Value}

import scala.jdk.CollectionConverters._
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

/**
  * Created by s on 10/24/16.
  */
class CallbackInstrumenter(config: Config, instrumentationClasses: scala.collection.mutable.Buffer[String]) extends BodyTransformer {
  val logCallbackEntrySig: String = "void logCallbackEntry(java.lang.String,java.lang.String,java.lang.String[],java.lang.String,java.lang.Object[],java.lang.String)"
  val applicationPackages = config.applicationPackages.map((a:String) =>{
    Utils.packageGlobToSignatureMatchingRegex(a).r
  })
  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]) = {
    val declaration: String = b.getMethod.getDeclaration
    val method: SootMethod = b.getMethod()
    val locals = method.getActiveBody.getLocals
    def addLocal(loc:Local):Unit = {
      if(!locals.contains(loc)){
        locals.add(loc)
      }
    }

    val name: String = b.getMethod.getName
    //val signature: String = b.getMethod.getSignature
    val signature: String = b.getMethod.getDeclaringClass.getName

//    val method_in_app: Boolean = applicationPackages.exists((r: Regex )=>{
//      signature match{
//        case r() => true
//        case _ => false
//      }
//    })
    val method_in_app: Boolean = Utils.sootClassMatches(b.getMethod.getDeclaringClass, config.applicationPackagesr.toSet) && !Utils.isFrameworkClass(signature)
    if(method_in_app && name != "<clinit>" && !method.isStatic) { //Is this a method we would like to instrument?
      val units: PatchingChain[soot.Unit] = b.getUnits;
      val paramCount: Int = method.getParameterCount

      val args = new scala.collection.mutable.ListBuffer[Local]()
      var lastArg: Option[soot.Unit] = None
      var thisRef: Option[soot.Unit] = None
      for (i: soot.Unit <- units.snapshotIterator().asScala) {

        i.apply(new AbstractStmtSwitch {
          private val methodSignature: String = method.getSubSignature

          override def caseReturnVoidStmt(stmt: ReturnVoidStmt): Unit = {
            val lsignature: Local = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
            addLocal(lsignature)
            val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
            addLocal(methodname)
            val logReturn: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
              .getMethod("void logCallbackReturn(java.lang.String,java.lang.String,java.lang.Object)")
            units.insertBefore(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),i)
            units.insertBefore(Jimple.v().newAssignStmt(methodname, StringConstant.v(methodSignature)),i)
            val returnTmp = Jimple.v().newLocal(Utils.nextName("returnTmp"), RefType.v("java.lang.Object"))
            addLocal(returnTmp)
            units.insertBefore(Jimple.v().newAssignStmt(returnTmp, NullConstant.v()),i)
            val (a:SootMethodRef,b) = (logReturn.makeRef(), List[Local](lsignature, methodname, returnTmp))
            units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(a,b:_*)),i)
          }
          override def caseReturnStmt(stmt: ReturnStmt): Unit = {
            stmt.getOp match{
              case l: Local => {
                val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
                addLocal(lsignature)
                val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
                addLocal(methodname)
                val logReturn: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
                  .getMethod("void logCallbackReturn(java.lang.String,java.lang.String,java.lang.Object)")
                units.insertBefore(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),i)
                units.insertBefore(Jimple.v().newAssignStmt(methodname, StringConstant.v(methodSignature)),i)
                val returnTmp = Jimple.v().newLocal(Utils.nextName("returnTmp"), RefType.v("java.lang.Object"))
                addLocal(returnTmp)
                units.insertBefore(Jimple.v().newAssignStmt(returnTmp, Utils.autoBox(l)),i)

                units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logReturn.makeRef(),
                  List[Local](lsignature, methodname, returnTmp):_*)),i)
              }
              case l: Constant => {
                val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
                addLocal(lsignature)
                val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
                addLocal(methodname)
                val logReturn: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
                  .getMethod("void logCallbackReturn(java.lang.String,java.lang.String,java.lang.Object)")
                units.insertBefore(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),i)
                units.insertBefore(Jimple.v().newAssignStmt(methodname, StringConstant.v(methodSignature)),i)
                val returnTmp = Jimple.v().newLocal(Utils.nextName("returnTmp"), RefType.v("java.lang.Object"))
                addLocal(returnTmp)
                units.insertBefore(Jimple.v().newAssignStmt(returnTmp, Utils.autoBox(l)),i)
                units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logReturn.makeRef(),
                  List[Value](lsignature, methodname, returnTmp):_*)),i)
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
          addLocal(inputArgs)
          //add call to entry method
          val logCallback: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
            .getMethod(logCallbackEntrySig)
          val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
          addLocal(lsignature)
          val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
          addLocal(methodname)
          val argTypes: Local = Jimple.v().newLocal(Utils.nextName("argTypes"), ArrayType.v(RefType.v("java.lang.String"),1))
          addLocal(argTypes)
          val returnType: Local = Jimple.v().newLocal(Utils.nextName("returnType"), RefType.v("java.lang.String"))
          addLocal(returnType)
          val simpleName: Local = Jimple.v().newLocal(Utils.nextName("simpleName"), RefType.v("java.lang.String"))
          addLocal(simpleName)

          val expr= Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logCallback.makeRef(), List[Local](lsignature, methodname,argTypes, returnType, inputArgs, simpleName):_*))//TODO: update method call
          units.insertAfter(expr,l)
          val newThisRef = if(b.getMethod.isStatic){NullConstant.v()} else{b.getThisLocal}
          val thisAssign = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(inputArgs, IntConstant.v(0)), newThisRef)
          units.insertAfter(thisAssign,l)

          units.insertAfter(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),l)
          val signature2: String = method.getSubSignature
          val simpleNameValue: String = method.getName
          units.insertAfter(Jimple.v().newAssignStmt(methodname, StringConstant.v(signature2)),l)
          units.insertAfter(Jimple.v().newAssignStmt(simpleName, StringConstant.v(simpleNameValue)),l)
          units.insertAfter(Jimple.v().newAssignStmt(returnType, StringConstant.v( method.getReturnType.toString)),l)


          //Convert method parameters to string locals that can be passed to callback entry logger
          (0 until method.getParameterCount).foreach(i =>{
            val pType = method.getParameterType(i)
            units.insertAfter(Jimple.v().newAssignStmt(Jimple.v().newArrayRef(argTypes, IntConstant.v(i)),
              StringConstant.v(pType.toString)),l)
          } )
          units.insertAfter(Jimple.v().newAssignStmt(argTypes, Jimple.v().newNewArrayExpr(RefType.v("java.lang.String"), IntConstant.v(method.getParameterCount))),l)



          //adding this ref to first element of inputArgs as receiver





          //Put arguments to callback into an array
          val tuples: ListBuffer[(Local, Int)] = args zip (1 to args.length).toList
          tuples.foreach((a: (Local,Int)) => {
            val tmpLocal: Local = Jimple.v().newLocal(Utils.nextName("inputArgs"), RefType.v("java.lang.Object"))
            addLocal(tmpLocal)

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
              addLocal(inputArgs)
              val newThisRef = if(b.getMethod.isStatic){NullConstant.v()} else{b.getThisLocal}
              val thisAssign = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(inputArgs, IntConstant.v(0)), newThisRef)
              val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
              addLocal(lsignature)
              val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
              addLocal(methodname)
              val logCallback: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
                .getMethod(logCallbackEntrySig)
              val argTypes: Local = Jimple.v().newLocal(Utils.nextName("argTypes"), ArrayType.v(RefType.v("java.lang.String"),1))
              addLocal(argTypes)
              val returnType: Local = Jimple.v().newLocal(Utils.nextName("returnType"), RefType.v("java.lang.String"))
              addLocal(returnType)
              val simpleName: Local = Jimple.v().newLocal(Utils.nextName("simpleName"), RefType.v("java.lang.String"))
              addLocal(simpleName)
              val expr= Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logCallback.makeRef(), List[Local](lsignature, methodname,argTypes ,returnType ,inputArgs, simpleName):_*))

              units.insertAfter(expr,thisRef)
              //TODO: assign return type
              units.insertAfter(Jimple.v().newAssignStmt(returnType,StringConstant.v(method.getReturnType.toString)),thisRef)

              //TODO: add argument types to array
              (0 until method.getParameterCount).foreach(i =>{
                val pType = method.getParameterType(i)
                units.insertAfter(Jimple.v().newAssignStmt(Jimple.v().newArrayRef(argTypes, IntConstant.v(i)),
                  StringConstant.v(pType.toString)),thisRef)
              } )

              //TODO: initialize array of argument types
              units.insertAfter(Jimple.v().newAssignStmt(argTypes, Jimple.v().newNewArrayExpr(RefType.v("java.lang.String"), IntConstant.v(method.getParameterCount))),thisRef)

              units.insertAfter(thisAssign, thisRef)
              units.insertAfter(
                Jimple.v().newAssignStmt(inputArgs, Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"),
                  IntConstant.v(args.length + 1))),
                thisRef)
              //Add call to entry method
//              val inputArgs: Local = Jimple.v().newLocal(Utils.nextName("inputArgs"), ArrayType.v(RefType.v("java.lang.Object"), 1))


              val signature1: String = method.getSubSignature
              val simpleNameVal: String = method.getName
              units.insertAfter(Jimple.v().newAssignStmt(methodname, StringConstant.v(signature1)),thisRef)
              units.insertAfter(Jimple.v().newAssignStmt(simpleName, StringConstant.v(simpleNameVal)), thisRef)
              units.insertAfter(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),thisRef)

            }
            case None => {
              ??? //Shouldn't happen
            }
          }
        }
      }

    }else if(name == "<clinit>" && method_in_app){
      val units: PatchingChain[soot.Unit] = b.getUnits
      assert(method.getParameterCount == 0) //static initializer should not have arguments

      //Log callback entry
      val logCallback: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
        .getMethod(logCallbackEntrySig)


      val simpleName: Local = Jimple.v().newLocal(Utils.nextName("simpleName"), RefType.v("java.lang.String"))
      addLocal(simpleName)
      val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
      addLocal(lsignature)
      val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
      addLocal(methodname)
      val argTypes: Local = Jimple.v().newLocal(Utils.nextName("argTypes"), ArrayType.v(RefType.v("java.lang.String"),1))
      addLocal(argTypes)
      val returnType: Local = Jimple.v().newLocal(Utils.nextName("returnType"), RefType.v("java.lang.String"))
      addLocal(returnType)
      val inputArgs: Local = Jimple.v().newLocal(Utils.nextName("inputArgs"), ArrayType.v(RefType.v("java.lang.Object"), 1))
      addLocal(inputArgs)
      val expr= Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logCallback.makeRef(), List[Local](lsignature,
        methodname,argTypes, returnType, inputArgs, simpleName):_*))
      units.addFirst(expr)
      units.addFirst(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)))
      units.addFirst(Jimple.v().newAssignStmt(methodname, StringConstant.v(method.getName)))
      units.addFirst(Jimple.v.newAssignStmt(argTypes,Jimple.v.newNewArrayExpr(RefType.v("java.lang.String"), IntConstant.v(0))))
      units.addFirst(Jimple.v().newAssignStmt(returnType,StringConstant.v(method.getReturnType.toString)))
      units.addFirst(Jimple.v().newAssignStmt(Jimple.v().newArrayRef(inputArgs, IntConstant.v(0)),
        NullConstant.v()))
      units.addFirst(Jimple.v.newAssignStmt(inputArgs, Jimple.v.newNewArrayExpr(RefType.v("java.lang.Object"), IntConstant.v(1))))
      units.addFirst(Jimple.v.newAssignStmt(simpleName, StringConstant.v(method.getName())))


      for (i: soot.Unit <- units.snapshotIterator().asScala) {

        i.apply(new AbstractStmtSwitch {
          private val methodSignature: String = method.getSubSignature

          override def caseReturnVoidStmt(stmt: ReturnVoidStmt): Unit = {
            val lsignature = Jimple.v().newLocal(Utils.nextName("callinSig"), RefType.v("java.lang.String"))
            addLocal(lsignature)
            val methodname = Jimple.v().newLocal(Utils.nextName("callinName"), RefType.v("java.lang.String"))
            addLocal(methodname)
            val logReturn: SootMethod = Scene.v().getSootClass(TraceRunnerOptions.CALLIN_INSTRUMENTATION_CLASS)
              .getMethod("void logCallbackReturn(java.lang.String,java.lang.String,java.lang.Object)")
            units.insertBefore(Jimple.v().newAssignStmt(lsignature, StringConstant.v(signature)),i)
            units.insertBefore(Jimple.v().newAssignStmt(methodname, StringConstant.v(methodSignature)),i)
            val returnTmp = Jimple.v().newLocal(Utils.nextName("returnTmp"), RefType.v("java.lang.Object"))
            addLocal(returnTmp)
            units.insertBefore(Jimple.v().newAssignStmt(returnTmp, NullConstant.v()),i)
            units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logReturn.makeRef(),
              List[Local](lsignature, methodname, returnTmp):_*)),i)
          }
        })}

    }
//    if(method.getDeclaringClass.getName == "com.example.row1antennapodrxjava.MainActivity" && method.getName == "<init>") {
//      println()
//    }
    method.getActiveBody.validate()
    //    method.getDeclaringClass.validate()
  }
}
