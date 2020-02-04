name := "Ifany"

version := "1.0"

scalaVersion := "2.13.1"

Compile / scalaSource := baseDirectory.value / "src"

scalacOptions ++= Seq("-unchecked", "-Ywarn-dead-code", "-deprecation")

libraryDependencies  ++= Seq(
    "ws.unfiltered" %% "unfiltered-netty-server" % "0.10.0-M6",
    "org.json4s" %% "json4s-native" % "3.6.7",
    "ws.unfiltered" %% "unfiltered-specs2" % "0.10.0-M6",
    "org.specs2" %% "specs2-core" % "4.6.0",
    "com.squareup.okhttp3" % "okhttp" % "4.0.0"
)

scalacOptions in Test ++= Seq("-Yrangepos")

// The main class
mainClass in (Compile, run) := Some("validator.Main")
