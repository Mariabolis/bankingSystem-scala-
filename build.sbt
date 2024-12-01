import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"



lazy val root = (project in file("."))
  .settings(
    name := "bankSystem",
      libraryDependencies ++= Seq(
        "com.typesafe.slick" %% "slick" % "3.4.1",
        "mysql" % "mysql-connector-java" % "8.0.33", // MySQL connector
        "com.typesafe.akka" %% "akka-actor" % "2.8.0", // Akka (for example)
        "com.typesafe" % "config" % "1.4.2", // Typesafe Config
        "ch.qos.logback" % "logback-classic" % "1.4.7",
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.20.0",
        "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
        "org.scalafx" %% "scalafx" % "16.0.0-R23"


      )
  )
