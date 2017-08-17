name := "global-snapshot-akka"

version := "1.0"

scalaVersion := "2.12.3"

lazy val akkaVersion = "2.5.4"
lazy val scalatestVersion = "3.0.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,

  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)
