package edu.colorad.cs.TraceRunner
//import soot.G;

import java.io.{File, OutputStream}
import java.nio.file.{Files, Path}
import java.util
import javax.tools.{JavaCompiler, StandardJavaFileManager, ToolProvider}

import edu.colorado._
import soot.jimple.NullConstant
import soot.{PackManager, PhaseOptions, Scene, SootClass, SootMethod, Transform, Type}
import soot.options.Options

import scala.collection.JavaConverters._
import scala.util.matching.Regex

case class Config(apkPath: String = null,
                  androidJars: String = null,
                  outputDir: String = null,
                  applicationPackages: Array[String] = Array(),
                  instDir: String = null,
                  jimpleOutput: Boolean = false
                 ){
  val applicationPackagesr = applicationPackages.map((a:String) =>{
    Utils.packageGlobToSignatureMatchingRegex(a).r
  })
//  @Deprecated
//  def isApplicationPackage(signature: String): Boolean = {
//    applicationPackagesr.exists((r: Regex) => {
//      signature match {
//        case r() => {
//          true
//        }
//        case _ => {
//          false
//        }
//      }
//    })
//  }
}

object TraceRunner {



  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("TraceRunner") {
      opt[String]('j', "android-jars").action( (x,c) => c.copy(androidJars=x))
        .text("Directory containing soot android jars").required()
      opt[String]('d', "process-dir").action( (x,c) => c.copy(apkPath=x))
        .text("Path to APK").required()
      opt[String]('o', "output-dir").action( (x,c) => c.copy(outputDir=x))
        .text("Output Directory").required()
      opt[String]('p', "application-packages").action((x,c) =>
        c.copy(applicationPackages = x.split(":").filter(a => a != "")))
      opt[String]('i', "instrumentation-directory").action((x,c) => c.copy(instDir = x)).required()
      opt[Unit]('m', "output_jimple").action((x,c) => c.copy(jimpleOutput = true))

    }
    parser.parse(args,Config()) match {
      case Some(config) => {
//        if(config.applicationPackages.size > 0) {

        /**hack to make osx openjdk work**/
        if(System.getProperty("os.name").equals("Mac OS X")){
          Scene.v().setSootClassPath(config.apkPath + ":" +
            sys.env("JAVA_HOME") + "/jre/lib/rt.jar")

        }
        //** Set Options **
        //prefer Android APK files// -src-prec apk
        Options.v().set_src_prec(Options.src_prec_apk)
        if(!config.jimpleOutput) {
          Options.v().set_output_format(Options.output_format_dex)
        }
        Options.v().set_android_jars(config.androidJars)
        if(TraceRunnerOptions.USE_PHANTOM_CLASSES) {
          Options.v().set_allow_phantom_refs(true)
        }

        Options.v().set_process_dir(List(config.apkPath).asJava)
        Options.v().set_output_dir(config.outputDir)
        Options.v().set_whole_program(true)



        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);



        /**add instrumentation to classpath**/
        val path: String = Scene.v().getSootClassPath

        Scene.v().setSootClassPath(path + ":" + config.instDir)

        val classes: scala.collection.mutable.Buffer[String] = JUtils.getClasses(config.instDir).asScala
        classes.foreach(a => Scene.v().addBasicClass(a))


        //Disable callgraph construction
        PhaseOptions.v().setPhaseOption("cg", "enabled:false");

        /** callin transformer**/
//        PackManager.v().getPack("jtp").add(
//          new Transform("jtp.callinInstrumenter", new CallinInstrumenter(config, classes)))
        PackManager.v().getPack("jtp").add(
          new Transform("jtp.callbackInstrumenter", new CallbackInstrumenter(config, classes)))

        /**run soot transformation**/
        val config1: Array[String] = TraceRunnerOptions.getSootConfig(config)
//        val value: Type = NullConstant.v().getType

        soot.Main.main(config1);
//        }else{
//          throw new IllegalArgumentException("Application packages must be non empty")
//        }
      }
      case None => {}
    }
	}
}

