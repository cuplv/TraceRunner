package edu.colorado

import java.util

import edu.colorad.cs.TraceRunner.Config
import soot.jimple.{Jimple, SpecialInvokeExpr}
import soot.util.NumberedString
import soot.{Body, Local, PatchingChain, Scene, SceneTransformer, SootClass, SootMethod, SootMethodRef, Type, Unit, VoidType}

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
//    name == "onPause"
//    false
//    !name.contains("<init>") &&
//     name(0) == 111 //111

//    name.startsWith("on") && name(2) == 77 //<=77 fail : <=76 works 77 M
//    name == ""
    true
//    name.startsWith("onMenuItemSelected") //menuitemselected
  }



  override def internalTransform(phaseName: String, options: util.Map[String, String]): scala.Unit = {
    val sc = Scene.v()
    sc.getApplicationClasses.iterator.foreach(applicationClass =>{
      if(!Utils.isFrameworkClass(applicationClass.getName)) {
        val superclass: SootClass = applicationClass.getSuperclass

        val methodsToOverride = getOverrideableMethodsChain(superclass, Set[SootMethod]()).foldLeft(Map[(String, List[Type], Boolean, Int), Type]())((acc, a) => {
          val parameterTypes: List[Type] = a.getParameterTypes.toList
          val mkey: (String, List[Type], Boolean, Int) = (a.getName, parameterTypes,a.isAbstract, a.getModifiers)
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
          val isAbstract: Boolean = methodTup._1._3
          val modifiers: Int = methodTup._1._4

          if((!applicationClass.declaresMethod(methodName,methodParams)) && dbgPred(methodName)) {
            ////          val method: SootMethod = applicationClass.declaresMethod(methodTup._1,methodTup._3)
            val newMethod: SootMethod = new SootMethod(methodName, methodParams, returnType) //TODO: accessor permissions?

            //Create new body for method
            val activeBody: Body = Jimple.v().newBody(newMethod)
            newMethod.setActiveBody(activeBody)
            dupModifiers(modifiers, newMethod)
            applicationClass.addMethod(newMethod)
            val units: PatchingChain[Unit] = activeBody.getUnits

//            if (!isAbstract) {
              val thisRef = Jimple.v().newLocal(Utils.nextName("this"), applicationClass.getType)
              activeBody.getLocals.add(thisRef)
              units.addLast(Jimple.v().newIdentityStmt(thisRef, Jimple.v().newThisRef(applicationClass.getType)))
              val parameterZip: mutable.Buffer[(Type, Int)] = newMethod.getParameterTypes zip (0 until newMethod.getParameterCount)
              //Copy parameters into locals
              val args = parameterZip
                .map(paramNumType => {
                  val paramType = paramNumType._1
                  val paramNum = paramNumType._2
                  val param = Jimple.v().newLocal(Utils.nextName("param"), paramType)
                  activeBody.getLocals.add(param)
                  units.addLast(Jimple.v().newIdentityStmt(param, Jimple.v().newParameterRef(paramType, paramNum)))

                  param
                })

              //Call super to units
              val method: SootMethodRef = getSuperMethod(applicationClass.getSuperclass, methodName,
                methodParams, returnType)

              val b: Boolean = activeBody.getThisLocal == thisRef
              val newSpecialInvokeExpr: SpecialInvokeExpr = Jimple.v().newSpecialInvokeExpr(thisRef, method, args)
              if (method.returnType() == VoidType.v()) {
                units.addLast(Jimple.v().newInvokeStmt(newSpecialInvokeExpr))
                units.addLast(Jimple.v().newReturnVoidStmt())
              } else {
                val returnVal = Jimple.v().newLocal(Utils.nextName("returnVal"), returnType)
                activeBody.getLocals.add(returnVal)
                units.addLast(Jimple.v().newAssignStmt(returnVal, newSpecialInvokeExpr))
                units.addLast(Jimple.v().newReturnStmt(returnVal))
              }
//            }else{
//              returnType match{
//                case i: VoidType => units.addLast(Jimple.v().newReturnVoidStmt())
//                case _ => ???
//              }
//            }
          }
        })

//        a.addMethod()
      }
    })
  }

  def dupModifiers(modifiers: Int, newMethod: SootMethod) = {

    newMethod.setModifiers(modifiers)
  }

  def getSuperMethod(clazz: SootClass, name: String, args: List[Type], returnType: Type): SootMethodRef = {
    clazz.getMethods()
    if(clazz.declaresMethod(name,args)){
      clazz.getMethod(name, args).makeRef()
    }else {
      Scene.v().makeMethodRef(clazz, name, args,returnType,false)
    }
  }

  final def getOverrideableMethodsChain(clazz: SootClass, exclude: Set[SootMethod]): Set[SootMethod] = {
    if(clazz.getName != "java.lang.Object"){
      val curOverrideableMethods: Set[SootMethod] = getOverrideableMethods(clazz, exclude)
      curOverrideableMethods.union(getOverrideableMethodsChain(clazz.getSuperclass, curOverrideableMethods.union(exclude)))
    }else Set[SootMethod]()
  }
  def getOverrideableMethods(clazz: SootClass, exclude: Set[SootMethod]): Set[SootMethod] = {
    clazz.getMethods.flatMap{(a: SootMethod) =>
      val excluded: Boolean = isExcluded(exclude, a)
      if(!a.isPrivate && !a.isStatic && !a.getDeclaringClass.isInterface && !a.isAbstract && !a.isFinal && !excluded){
        Some(a)
      }else None
    }.toSet
  }
  def isExcluded(exclude: Set[SootMethod], method: SootMethod): Boolean ={
    val ret = !exclude.exists(a => {
      if(a.getName == method.getName){
        if((a.getParameterTypes zip method.getParameterTypes).exists(t => t._1 != t._2)){
          false
        }else{
          true
        }
      }else{
        false
      }
    })
    if(exclude.isEmpty) false else ret
  }
}
