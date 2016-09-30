package edu.colorado

import java.util

import edu.colorad.cs.TraceRunner.Config
import soot.jimple.{InvokeStmt, AbstractStmtSwitch}
import soot.{PatchingChain, Body, BodyTransformer}
import scala.collection.JavaConversions._

/**
 * Created by s on 9/21/16.
 */
class CallinInstrumenter(config: Config) extends BodyTransformer{
  override def internalTransform(b: Body, phaseName: String, options: util.Map[String, String]): Unit = {


    println("------------")
    println("declaration")
    println(b.getMethod.getDeclaration)
    println("method")
    println(b.getMethod())
    println("method name")
    println(b.getMethod.getName)
    println("method signature")
    println(b.getMethod.getSignature)
    println("=============")

    b.getMethod.getDeclaringClass.getName

    val units: PatchingChain[soot.Unit] = b.getUnits;
    for (i:soot.Unit <- units.snapshotIterator()){
      i.apply(new AbstractStmtSwitch {
        override def caseInvokeStmt(stmt:InvokeStmt) = {
//          println(stmt) //TODO
        }
      })
    }
  }
}
