package edu.colorado

import java.util

import edu.colorad.cs.TraceRunner.Config
import soot.jimple.Jimple
import soot.util.NumberedString
import soot.{Body, Local, PatchingChain, Scene, SceneTransformer, SootClass, SootMethod, SootMethodRef, Type, Unit}

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable


/**
  * Created by s on 11/3/16.
  */
class OverrideAllMethods(config: Config) extends SceneTransformer {
  def dbgPred(name: String): Boolean = {
//    name.size > 3 && name(0) == 'a' && name(1)=='d' && name(2) == 'd'
//    name == "addContentView"
    name == "onPause"
//    false
  }



  override def internalTransform(phaseName: String, options: util.Map[String, String]): scala.Unit = {
    val sc = Scene.v()
    sc.getApplicationClasses.iterator.foreach(applicationClass =>{
      if(!Utils.isFrameworkClass(applicationClass.getName)) {
        val superclass: SootClass = applicationClass.getSuperclass
        val methodsToOverride = getOverrideableMethodsChain(superclass).foldLeft(Map[(String, List[Type]), Type]())((acc, a) => { //TODO: map from method name/type to return
          val parameterTypes: List[Type] = a.getParameterTypes.toList
          val mkey: (String, List[Type]) = (a.getName, parameterTypes)
          if(acc.contains(mkey)) {
            val returnTypeInMap: Type = acc(mkey)
            if (a.getReturnType == returnTypeInMap) {
              acc
            } else {
              //Case where return types do not match
              //TODO: Implement this. Probably can happen with generics, how else can this happen?
              //Ideas: take less precise type and override that
              //If type doesn't intersect then what?
              val returnTypeName: String = returnTypeInMap.getEscapedName
              Scene.v().getSootClass(returnTypeName)

              ???
            }
          }else{
            acc + (mkey -> a.getReturnType)
          }
        })
        methodsToOverride.foreach(methodTup => {
          val methodName: String = methodTup._1._1
          val methodParams: List[Type] = methodTup._1._2
          val returnType: Type = methodTup._2

          if(!applicationClass.declaresMethod(methodName,methodParams) && dbgPred(methodName)){
////          val method: SootMethod = applicationClass.declaresMethod(methodTup._1,methodTup._3)
            val newMethod: SootMethod = new SootMethod(methodName, methodParams, returnType) //TODO: accessor permissions?

            //Create new body for method
            val activeBody: Body = Jimple.v().newBody(newMethod)
            newMethod.setActiveBody(activeBody)
            applicationClass.addMethod(newMethod)
            val units: PatchingChain[soot.Unit] = activeBody.getUnits


            val thisRef = Jimple.v().newLocal("this", applicationClass.getType)
            activeBody.getLocals.add(thisRef)
            units.addLast(Jimple.v().newIdentityStmt(thisRef, Jimple.v().newThisRef(applicationClass.getType)))
            val parameterZip: mutable.Buffer[(Type, Int)] = newMethod.getParameterTypes zip (0 until newMethod.getParameterCount)
            //TODO:Copy parameters into locals
            val args = parameterZip
              .map(paramNumType => {
              val paramType = paramNumType._1
              val paramNum = paramNumType._2
              val param = Jimple.v().newLocal(Utils.nextName("param"), paramType)
              activeBody.getLocals.add(param)
              units.addLast(Jimple.v().newIdentityStmt(param,Jimple.v().newParameterRef(paramType, paramNum)))

              param
            })

            //TODO:Call super to units
            val method: SootMethodRef = getSuperMethod(applicationClass.getSuperclass, methodName,
              methodParams, returnType)

            val b: Boolean = activeBody.getThisLocal == thisRef
            units.addLast(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisRef, method ,args)))
          }
        })

//        a.addMethod()
      }
    })
  }
  def getSuperMethod(clazz: SootClass, name: String, args: List[Type], returnType: Type): SootMethodRef = {
    clazz.getMethods()
    if(clazz.declaresMethod(name,args)){
      clazz.getMethod(name, args).makeRef()
    }else {
      Scene.v().makeMethodRef(clazz, name, args,returnType,false)
    }
  }
  final def getOverrideableMethodsChain(clazz: SootClass): Set[SootMethod] = {
    if(clazz.getName != "java.lang.Object"){
      getOverrideableMethods(clazz).union(getOverrideableMethodsChain(clazz.getSuperclass))
    }else Set[SootMethod]()
  }
  def getOverrideableMethods(clazz: SootClass): Set[SootMethod] = {
    clazz.getMethods.flatMap{(a: SootMethod) =>
      if(!a.isPrivate && !a.isStatic && !a.getDeclaringClass.isInterface){
        Some(a)
      }else None
    }.toSet
  }
}
