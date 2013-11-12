import sbt._
import Keys._

object LandscapeBuild extends Build {

  val appName         = "Landscape"
  val appVersion      = "0.0.1-SNAPSHOT"

  object Version {
    val astyanax = "1.56.42"
    val scala     = "2.10.3"
    val scalaTest = "2.0.RC1-SNAP6"
  }

  val appDependencies =  Seq(
    "com.netflix.astyanax" % "astyanax-core" % Version.astyanax /*exclude("org.slf4j", "slf4j-log4j12")*/,
    "com.netflix.astyanax" % "astyanax-thrift" % Version.astyanax exclude("javax.servlet", "servlet-api"),
    "com.netflix.astyanax" % "astyanax-entity-mapper" % Version.astyanax /*exclude("org.slf4j", "slf4j-log4j12")*/
  )

  val testDependencies = Seq(
    "org.scalatest" %% "scalatest" % Version.scalaTest % "test",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.3" % "test"
  )

  val buildSettings = Defaults.defaultSettings ++ Seq(
    version := appVersion,
    exportJars := true,
    scalaVersion        := Version.scala,
    javacOptions ++= Seq("-source", "1.6") ++ Seq("-target", "1.6"),
    unmanagedClasspath in Runtime += file("conf/")
  )

  val main = Project(id = appName, base = file("."),
    settings = buildSettings ++ Seq(libraryDependencies ++= (appDependencies ++ testDependencies))
  )
}
