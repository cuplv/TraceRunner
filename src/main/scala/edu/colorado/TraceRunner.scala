package edu.colorad.cs.TraceRunner
//import soot.G;

import java.io.OutputStream
import java.security.spec.AlgorithmParameterSpec


import edu.colorado.{TraceRunnerOptions, CallinInstrumenter}
import soot.{Transform, PackManager, Scene, SootClass}
import soot.options.Options;

case class Config(apkPath: String = null,
                  androidJars: String = null,
                  outputDir: String = null,
                  applicationPackages: Array[String] = null)

object TraceRunner {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Config]("TraceRunner") {
      opt[String]('j', "android-jars").action( (x,c) => c.copy(androidJars=x))
        .text("Directory containing soot android jars").required()
      opt[String]('d', "process-dir").action( (x,c) => c.copy(apkPath=x))
        .text("Path to APK").required()
      opt[String]('o', "output-dir").action( (x,c) => c.copy(outputDir=x))
        .text("Output Directory").required()
      opt[String]('p', "Application Packages").action((x,c) =>
        c.copy(applicationPackages = x.split(":").filter(a => a != "")))

    }
    parser.parse(args,Config()) match {
      case Some(config) => {
        //prefer Android APK files// -src-prec apk
        Options.v().set_src_prec(Options.src_prec_apk)
        //output as APK, too//-f J
        Options.v().set_output_format(Options.output_format_dex)
        // resolve the PrintStream and System soot-classes
        Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);
        PackManager.v().getPack("jtp").add(
          new Transform("jtp.myInstrumenter", new CallinInstrumenter(config)));

        val config1: Array[String] = TraceRunnerOptions.getSootConfig(config)
        soot.Main.main(config1);
      }
      case None => {}
    }





	}
}

