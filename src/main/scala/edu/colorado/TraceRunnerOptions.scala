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

  def getSootConfig(config: Config): Array[String] = {
    Array(
      if(USE_PHANTOM_CLASSES){
        Some("-allow-phantom-refs")
      }else{None},
      if(config.jimpleOutput){Some("-output-format")} else None,
      if(config.jimpleOutput){Some("jimple")} else None,
      Some("-android-jars"),
      Some(config.androidJars),
      Some("-process-dir"),
      Some(config.apkPath),
      Some("-output-dir"),
      Some(config.outputDir),
      Some("-w") //-w option does whole program which enables SceneTransformers for InstrumentationGenerators

    ).flatten
  }
}
