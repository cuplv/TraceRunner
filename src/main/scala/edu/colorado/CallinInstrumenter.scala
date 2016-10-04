package edu.colorado

import java.util

import edu.colorad.cs.TraceRunner.Config
import soot.jimple.{InvokeStmt, AbstractStmtSwitch}
import soot.{SootMethod, PatchingChain, Body, BodyTransformer}
import scala.collection.JavaConversions._
import scala.util.matching.Regex

/**
 * Created by s on 9/21/16.
 */
class CallinInstrumenter(config: Config) extends BodyTransformer{

  val applicationPackages = config.applicationPackages.map((a:String) =>{
    Utils.packageGlobToSignatureMatchingRegex(a).r
  })

  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]): Unit = {


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
            //          println(stmt) //TODO
          }
        })
      }
    }else {}
  }
}
