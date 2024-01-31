//import com.github.retronym.SbtOneJar._

lazy val commonSettings = Seq(
  organization := "edu.colorado",
  version := "0.1.0",
  scalaVersion := "2.13.8",
  name := "Example",
  javaOptions += "-Xmx32G"
)

scalaVersion := "2.13.8"

libraryDependencies += "com.github.scopt" % "scopt_2.13" % "4.0.0-RC2" % "compile"

//oneJarSettings
test in assembly := {}

// ignore duplicate slf4j during assembly of fat jar
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

mainClass in assembly := Some("edu.colorado.TraceRunner.TraceRunner")
libraryDependencies += "com.github.pathikrit" % "better-files_2.13" % "3.9.1"

libraryDependencies += "ca.mcgill.sable" % "soot" % "4.1.0"
