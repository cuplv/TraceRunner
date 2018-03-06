import com.github.retronym.SbtOneJar._

lazy val commonSettings = Seq(
  organization := "edu.colorado",
  version := "0.1.0",
  scalaVersion := "2.11.8",
  name := "Example",
  javaOptions += "-Xmx32G"
)

scalaVersion := "2.11.8"

libraryDependencies += "com.github.scopt" % "scopt_2.11" % "3.5.0"

oneJarSettings
