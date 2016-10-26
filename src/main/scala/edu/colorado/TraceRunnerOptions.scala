package edu.colorado

import edu.colorad.cs.TraceRunner.Config

/**
 * Created by s on 9/29/16.
 * -allow-phantom-refs
 * -android-jars /Users/s/Documents/source/android-platforms
 * -process-dir /Users/s/Documents/source/TraceRunner/testApps/TestApp/app/build/outputs/apk/app-debug.apk
 * -output-dir /Users/s/Documents/source/TraceRunner/testApps/output
 */
object TraceRunnerOptions {
  val  USE_PHANTOM_CLASSES = true
  val CALLIN_INATRUMENTATION_CLASS: String =
    "edu.colorado.plv.tracerunner_runtime_instrumentation.TraceRunnerRuntimeInstrumentation"
  val FRAMEWORK_FILTER_FILE: String = "./resources/android_packages.txt"
  val source = scala.io.Source.fromFile(FRAMEWORK_FILTER_FILE)
  val filter_lines = (try source.mkString finally source.close()).split("\n")
  def getSootConfig(config: Config): Array[String] = {
    Array(
      if(config.jimpleOutput){Some("-output-format")} else None,
      if(config.jimpleOutput){Some("jimple")} else None,
      Some("-w")
    ).flatten
  }
}
