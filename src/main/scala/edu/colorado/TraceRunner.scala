package edu.colorad.cs.TraceRunner
//import soot.G;

import java.io.{File, OutputStream}
import java.nio.file.{Files, Path}
import javax.tools.{JavaCompiler, StandardJavaFileManager, ToolProvider}

import edu.colorado.{CallinInstrumenter, TraceRunnerOptions}
import soot.{PackManager, Scene, SootClass, Transform}
import soot.options.Options;
import scala.collection.JavaConverters._

case class Config(apkPath: String = null,
                  androidJars: String = null,
                  outputDir: String = null,
                  applicationPackages: Array[String] = null,
                  instDir: String = null
                 )

object TraceRunner {

  def instrumentationClasses(config: Config): Array[String] = {

    val r = ".*\\.class".r
    /** remove old instrumentation classes **/

    val instDirectory: File = new File(config.instDir)
    instDirectory.listFiles().map(a => {
      val name: String = a.getName
      name match{
        case r() => {
          Files.delete(a.toPath)
        }
        case _ => {}
      }
    })

    /** compile instrumentation classes **/
    val compiler: JavaCompiler = ToolProvider.getSystemJavaCompiler
    val standardFileManager: StandardJavaFileManager = compiler.getStandardFileManager(null,null,null)
    val fileObjects = standardFileManager.getJavaFileObjectsFromFiles(instDirectory.listFiles().toIterable.asJava)
    compiler.getTask(null, standardFileManager, null,null, null, fileObjects).call()

    /** list all instrumentation classes **/
    instDirectory.listFiles().filter(a =>{
      a match {
        case r() => true
        case _ => false
      }
    }).map(a => a.getAbsolutePath)
  }

  def addInstrumentationClasses(instrumentationClasses: Array[String]) = {
    Scene.v().loadClassAndSupport("java.lang.Object")
    Scene.v().loadClassAndSupport("java.lang.System")
    instrumentationClasses.map(a => Scene.v().loadClassAndSupport(a))
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("TraceRunner") {
      opt[String]('j', "android-jars").action( (x,c) => c.copy(androidJars=x))
        .text("Directory containing soot android jars").required()
      opt[String]('d', "process-dir").action( (x,c) => c.copy(apkPath=x))
        .text("Path to APK").required()
      opt[String]('o', "output-dir").action( (x,c) => c.copy(outputDir=x))
        .text("Output Directory").required()
      opt[String]('p', "Application Packages").action((x,c) =>
        c.copy(applicationPackages = x.split(":").filter(a => a != ""))).required()
      opt[String]('i', "Instrumentation Directory").action((x,c) => c.copy(instDir = x)).required()

    }
    parser.parse(args,Config()) match {
      case Some(config) => {
        if(config.applicationPackages.size > 0) {

          /**hack to make osx openjdk work**/
          if(System.getProperty("os.name").equals("Mac OS X")){
            Scene.v().setSootClassPath(sys.env("JAVA_HOME") + "/jre/lib/rt.jar")

          }

          /**add instrumentation to classpath**/
          val path: String = Scene.v().getSootClassPath
          val left: String =
            instrumentationClasses(config).foldLeft(path)((acc: String,v: String) =>
              acc + ":" + config.instDir + "/" + v)
          Scene.v().setSootClassPath(left)



          //** add instrumentation classes **
          addInstrumentationClasses(instrumentationClasses(config))


          //** Instrument callbacks **
          //prefer Android APK files// -src-prec apk
          Options.v().set_src_prec(Options.src_prec_apk)
          //output as APK, too//-f J
          Options.v().set_output_format(Options.output_format_force_dex)
          // resolve the PrintStream and System soot-classes
          Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
          Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);

          PackManager.v().getPack("jtp").add(
            new Transform("jtp.myInstrumenter", new CallinInstrumenter(config)));

          val config1: Array[String] = TraceRunnerOptions.getSootConfig(config)
          soot.Main.main(config1);
        }else{
          throw new IllegalArgumentException("Application packages must be non empty")
        }
      }
      case None => {}
    }
	}
}

