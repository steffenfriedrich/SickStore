name := "sickstore"
organization := "de.uni-hamburg"
version := "1.0"
scalaVersion := "2.11.6"
// Enables publishing to maven repo
publishMavenStyle := true
crossPaths := false

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % "test",
  "com.esotericsoftware" % "kryonet" % "2.22.0-RC1",
  "commons-cli" % "commons-cli" % "1.3",
  "com.google.guava" % "guava" % "18.0",
  "org.reflections" % "reflections" % "0.9.10",
  "org.apache.logging.log4j" % "log4j-api" % "2.3",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "io.dropwizard.metrics" % "metrics-core" % "3.1.2")

mainClass in (Compile, run) := Some("de.unihamburg.sickstore.main.Server")

/**
 * Scala Compiler Options If this project is only a subproject,
 * add these to a common project setting.
 */
scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code"
)

// see https://github.com/xerial/sbt-pack
packAutoSettings


