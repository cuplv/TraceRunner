package edu.colorado

import java.util

import edu.colorad.cs.TraceRunner.Config
import soot.{Body, BodyTransformer, PatchingChain, SootMethod}

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

      units.addFirst(???)


      units.addLast(???)
    }
  }
}
