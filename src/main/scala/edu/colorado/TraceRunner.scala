package edu.colorad.cs.TraceRunner
//import soot.G;


import edu.colorado._
import soot.options.Options
import soot.{PackManager, PhaseOptions, Scene, SootClass, Transform}

import scala.collection.JavaConverters._
import scala.util.matching.Regex

case class Config(apkPath: String = null,
                  androidJars: String = null,
                  outputDir: String = null,
                  applicationPackages: Array[String] = Array(),
                  instDir: String = null,
                  excludeClasses: Set[Regex] = Set(),
                  jimpleOutput: Boolean = false,
                  useJava: Boolean = false,
                  classOutput: Boolean = false
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
      opt[String]('x', "exclude-classes").action((x,c) =>
        c.copy(excludeClasses = Utils.semicolonSeparatedGlobsToRegex(x)))
      opt[Unit]('m', "output_jimple").action((x,c) => c.copy(jimpleOutput = true))
      opt[Unit]('c', "output_class").action((x,c) => c.copy(classOutput = true))
      opt[Unit]('v', "use_java").action((x,c) => c.copy(useJava = true))

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
        if(!config.useJava) {
          Options.v().set_src_prec(Options.src_prec_apk)
          if(config.classOutput){
            Options.v().set_output_format(Options.output_format_class)
          }else if (!config.jimpleOutput) {
            Options.v().set_output_format(Options.output_format_dex)
          }
          Options.v().set_android_jars(config.androidJars)
          if (TraceRunnerOptions.USE_PHANTOM_CLASSES) {
            Options.v().set_allow_phantom_refs(true)
          }
          Options.v().set_whole_program(true)
        }else{
          Options.v().set_src_prec(Options.src_prec_java)
          Options.v().set_allow_phantom_refs(true)
//          Scene.v().loadNecessaryClasses();
//          Scene.v().addBasicClass("java.io.InterruptedIOException", SootClass.SIGNATURES);
//          Options.v().set_whole_program(true);
        }

        Options.v().set_process_dir(List(config.apkPath).asJava)
        Options.v().set_output_dir(config.outputDir)




//        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
//        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);



        /**add instrumentation to classpath**/
        val path: String = Scene.v().getSootClassPath
//":/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar")
        Scene.v().setSootClassPath(path + ":" + config.instDir +":" + sys.env("JAVA_HOME") + "/jre/lib/rt.jar")

        val classes: scala.collection.mutable.Buffer[String] = JUtils.getClasses(config.instDir).asScala
        classes.foreach(a => Scene.v().addBasicClass(a))


        //Disable callgraph construction
        PhaseOptions.v().setPhaseOption("cg", "enabled:false");

        /** transformers**/
//        PackManager.v().getPack("wjtp").add(new Transform("wjtp.overrideallmethods", new OverrideAllMethods(config)))
//        PackManager.v().getPack("jtp").add(new Transform("jtp.callinInstrumenter", new CallinInstrumenter(config, classes)))
        PackManager.v().getPack("jtp").add(new Transform("jtp.callbackInstrumenter", new CallbackInstrumenter(config, classes)))
//        PackManager.v().getPack("jtp").add(new Transform("jtp.exceptionInstrumenter", new ExceptionInstrumenter(config,classes)))

        /**run soot transformation**/
        val config1: Array[String] = TraceRunnerOptions.getSootConfig(config)

        soot.Main.main(config1);

      }
      case None => {}
    }
	}
}

