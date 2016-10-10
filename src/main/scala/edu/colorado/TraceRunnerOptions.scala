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
      if(config.jimpleOutput){Some("-output-format")} else None,
      if(config.jimpleOutput){Some("jimple")} else None,
      Some("-w")
    ).flatten
  }
}
