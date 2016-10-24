package edu.colorado

import java.util

import edu.colorad.cs.TraceRunner.Config
import soot.{Body, BodyTransformer}

/**
  * Created by s on 10/24/16.
  */
class CallbackInstrumenter(config: Config, instrumentationClasses: scala.collection.mutable.Buffer[String]) extends BodyTransformer {
  val applicationPackages = config.applicationPackages.map((a:String) =>{
    Utils.packageGlobToSignatureMatchingRegex(a).r
  })
  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]) = {
    
  }
}
