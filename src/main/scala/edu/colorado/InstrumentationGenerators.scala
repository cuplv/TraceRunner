package edu.colorado


import java.io.{FileOutputStream, OutputStream, OutputStreamWriter, PrintWriter}
import java.util

import soot.jimple.{Jimple, JimpleBody, StringConstant}
import soot.options.Options
import soot.util.JasminOutputStream
import soot.{ArrayType, Local, Modifier, PatchingChain, RefType, Scene, SceneTransformer, SootClass, SootMethod, SourceLocator, Type, Unit, VoidType}

import scala.collection.JavaConverters._
//import scala.sys.process.processInternal.OutputStream

/**
  * Created by s on 10/6/16.
  */
class InstrumentationGenerators extends SceneTransformer{
  def addInstrumentationClasses() = {
    Scene.v().loadClassAndSupport("java.lang.Object")
    Scene.v().loadClassAndSupport("java.lang.System")
    val instClass = new SootClass("TraceRunnerRuntimeInstrumentation", Modifier.PUBLIC)
    instClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"))
    Scene.v().addClass(instClass)

    //create new method with body
    val method: SootMethod = new SootMethod("main",
      List[Type](ArrayType.v(RefType.v("java.lang.String"), 1)).asJava,
      VoidType.v(), Modifier.PUBLIC | Modifier.STATIC)
    instClass.addMethod(method)
    val body:JimpleBody = Jimple.v().newBody(method)
    method.setActiveBody(body)

    //add a local
    val arg = Jimple.v().newLocal("l0", ArrayType.v(RefType.v("java.lang.String"), 1))
    body.getLocals().add(arg)

    //Adding a Unit
    val units: PatchingChain[Unit] = body.getUnits
    units.add(Jimple.v().newIdentityStmt(arg,
      Jimple.v().newParameterRef(ArrayType.v
      (RefType.v("java.lang.String"), 1), 0)));
    val toCall = Scene.v().getMethod("<java.io.PrintStream: void println(java.lang.String)>");
    val tmpRef: Local = Jimple.v().newLocal("tmpRef", method.getReturnType())

    units.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), StringConstant.v("Hello world!"))));


    //Write out to file: TODO: this may break apk output... again
//    val fileName: String = SourceLocator.v().getFileNameFor(instClass, Options.output_format_class);
//    val streamOut: OutputStream = new JasminOutputStream(new FileOutputStream(fileName));
//    val writerOut: PrintWriter  = new PrintWriter(new OutputStreamWriter(streamOut));
  }

  override def internalTransform(phaseName: String, options: util.Map[String, String]): scala.Unit = {
    val _ = addInstrumentationClasses()
  }
}
