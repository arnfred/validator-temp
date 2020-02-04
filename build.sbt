name := "Ifany"

version := "1.0"

scalaVersion := "2.13.1"

Compile / scalaSource := baseDirectory.value / "src"

scalacOptions ++= Seq("-unchecked", "-Ywarn-dead-code", "-deprecation")

libraryDependencies  ++= Seq(
    "ws.unfiltered" %% "unfiltered-netty-server" % "0.10.0-M6",
    "ws.unfiltered" %% "unfiltered-specs2" % "0.10.0-M6" % "test",
    "org.json4s" %% "json4s-native" % "3.6.7"
    )

// The main class
mainClass in (Compile, run) := Some("validator.Main")
