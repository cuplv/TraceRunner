lazy val commonSettings = Seq(
  organization := "edu.colorado",
  version := "0.1.0",
  scalaVersion := "2.11.2",
  name := "Example",
  javaOptions += "-Xmx32G"
)



libraryDependencies += "org.smali" % "dexlib2" % "2.1.3"
