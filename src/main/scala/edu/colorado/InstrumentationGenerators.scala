package edu.colorado

import java.lang.reflect.Modifier
import java.util

import soot.{Scene, SceneTransformer, SootClass}

/**
  * Created by s on 10/6/16.
  */
class InstrumentationGenerators extends SceneTransformer{
  def addInstrumentationClasses() = {
    Scene.v().loadClassAndSupport("java.lang.Object")
    Scene.v().loadClassAndSupport("java.lang.System")
    val instClass = new SootClass("TraceRunnerRuntimeInstrumentation", Modifier.PUBLIC);
    instClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"))
    Scene.v().addClass(instClass);
  }

  override def internalTransform(phaseName: String, options: util.Map[String, String]): Unit = {
    addInstrumentationClasses()
  }
}
