name := "LibTrace"

version := "1.0"

scalaVersion := "2.11.11"

organization := "edu.colorado.plv"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

// (optional) If you need scalapb/scalapb.proto or anything from
// google/protobuf/*.proto
libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"
//libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "dk.brics.automaton" % "automaton" % "1.11-8"
libraryDependencies += "guru.nidi" % "graphviz-java" % "0.2.2"