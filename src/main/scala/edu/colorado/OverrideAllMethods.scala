package edu.colorado

import java.util

import edu.colorado.TraceRunner.Config
import soot.jimple.{Jimple, SpecialInvokeExpr}
import soot.util.NumberedString
import soot.{Body, Hierarchy, Local, PatchingChain, Scene, SceneTransformer, SootClass, SootMethod, SootMethodRef, Type, Unit, VoidType}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.collection.mutable


/**
  * Created by s on 11/3/16.
  */
class OverrideAllMethods(config: Config) extends SceneTransformer {
  val excluded: List[(String,String)] = List()
  def blacklisted(a: SootMethod): Boolean = {
    val methodname: String = a.getName
    val classname: String = a.getDeclaringClass.getName
    if (config.noOverrideGet) {
      methodname.startsWith("get")
    } else {
      false
    }
  }
  def dbgPred(name: String): Boolean = {

//    >100
    // <= 106
    // <= 103
//    name(0) > 101
//    name(0) > 101
//    name(0) == 104
//    name(0) > 104
//    true
    true
  }



  override def internalTransform(phaseName: String, options: util.Map[String, String]): scala.Unit = {
    val sc = Scene.v()
    sc.getApplicationClasses.iterator.asScala.foreach(applicationClass =>{
      if(Utils.sootClassMatches(applicationClass, config.applicationPackagesr.toSet)
        && !Utils.isFrameworkClass(applicationClass.getName) &&
        !Utils.sootClassMatches(applicationClass, config.excludeClasses)) {
        val superclass: SootClass = applicationClass.getSuperclass

        val methodsToOverride = getOverrideableMethodsChain(superclass, Set[SootMethod]())
          .foldLeft(Map[(String, List[Type], Boolean, Int), Type]())((acc, a) => {
          val parameterTypes: List[Type] = a.getParameterTypes.asScala.toList
          val mkey: (String, List[Type], Boolean, Int) = (a.getName, parameterTypes,a.isAbstract, a.getModifiers)
          if(acc.contains(mkey)) {
            val returnTypeInMap: Type = acc(mkey)
            val activeHierarchy: Hierarchy = Scene.v().getActiveHierarchy
            acc //iteration is from most precise type that is overridden so if key is contained then this will override correctly
          }else{
            acc + (mkey -> a.getReturnType)
          }
        })
        methodsToOverride.foreach(methodTup => {
          val methodName: String = methodTup._1._1
          val methodParams = methodTup._1._2.asJava
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
              val thisRef = Jimple.v().newLocal(Utils.nextName("this"), applicationClass.getType)
              activeBody.getLocals.add(thisRef)
              units.addLast(Jimple.v().newIdentityStmt(thisRef, Jimple.v().newThisRef(applicationClass.getType)))
              val parameterZip: mutable.Buffer[(Type, Int)] = newMethod.getParameterTypes.asScala zip (0 until newMethod.getParameterCount)
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
                methodParams.asScala.toList, returnType)

              val b: Boolean = activeBody.getThisLocal == thisRef
              val newSpecialInvokeExpr: SpecialInvokeExpr = Jimple.v().newSpecialInvokeExpr(thisRef, method, args.asJava)
              if (method.returnType() == VoidType.v()) {
                units.addLast(Jimple.v().newInvokeStmt(newSpecialInvokeExpr))
                units.addLast(Jimple.v().newReturnVoidStmt())
              } else {
                val returnVal = Jimple.v().newLocal(Utils.nextName("returnVal"), returnType)
                activeBody.getLocals.add(returnVal)
                units.addLast(Jimple.v().newAssignStmt(returnVal, newSpecialInvokeExpr))
                units.addLast(Jimple.v().newReturnStmt(returnVal))
              }
          }
        })
      }
    })
  }

  def dupModifiers(modifiers: Int, newMethod: SootMethod) = {

    newMethod.setModifiers(modifiers)
  }

  def getSuperMethod(clazz: SootClass, name: String, args: List[Type], returnType: Type): SootMethodRef = {
    clazz.getMethods()
    if(clazz.declaresMethod(name,args.asJava)){
      try {
        clazz.getMethod(name, args.asJava).makeRef()
      }catch{
        case t: Throwable => {
          clazz.getMethodUnsafe(name,args.asJava,returnType).makeRef()
        }
      }
    }else {
      Scene.v().makeMethodRef(clazz, name, args.asJava,returnType,false)
    }
  }

  final def getOverrideableMethodsChain(clazz: SootClass, exclude: Set[SootMethod]): Set[SootMethod] = {
    if(clazz == null){
      return Set()
    }
    if(clazz.getName != "java.lang.Object" && clazz.getName != "java.lang.Thread"){
      val curOverrideableMethods: Set[SootMethod] = getOverrideableMethods(clazz, exclude)
      curOverrideableMethods.union(getOverrideableMethodsChain(clazz.getSuperclass, clazz.getMethods.asScala.toSet.union(exclude)))
    }else Set[SootMethod]()
  }



  def getOverrideableMethods(clazz: SootClass, exclude: Set[SootMethod]): Set[SootMethod] = {
    if(Utils.isFrameworkClass(clazz)) {
      clazz.getMethods.asScala.flatMap { (a: SootMethod) =>
        val excluded: Boolean = isExcluded(exclude, a) || blacklisted(a)
        if (!a.isPrivate && !a.isStatic && !a.getDeclaringClass.isInterface && !a.isAbstract && !a.isFinal && !excluded) {
          Some(a)
        } else None
      }.toSet
    }else{
      Set[SootMethod]()
    }
  }
  def isExcluded(exclude: Set[SootMethod], method: SootMethod): Boolean ={
    val ret = exclude.exists(a => {
      val name: String = method.getName
      val name1: String = a.getName
      if(name1 == name){
        if(a.getParameterTypes.size != 0 &&(a.getParameterTypes.asScala zip method.getParameterTypes.asScala).exists(t => t._1 != t._2)){
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