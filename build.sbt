import com.github.retronym.SbtOneJar._

lazy val commonSettings = Seq(
  organization := "edu.colorado",
  version := "0.1.0",
  scalaVersion := "2.11.8",
  name := "Example",
  javaOptions += "-Xmx32G"
)

scalaVersion := "2.11.8"

//libraryDependencies += "org.smali" % "dexlib2" % "2.1.3"

//libraryDependencies += "org.ow2.asm" % "asm-debug-all" % "5.1"

libraryDependencies += "com.github.scopt" % "scopt_2.11" % "3.5.0"

libraryDependencies += "ca.mcgill.sable" % "soot" % "trunk"
oneJarSettings
