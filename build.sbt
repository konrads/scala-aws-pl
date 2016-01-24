scalaVersion := "2.11.7"

name := """scala-aws-pl"""

version := "1.0-SNAPSHOT"

lazy val root = project.in(file(".")).enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator

resolvers += Resolver.bintrayRepo("dwhjames", "maven")

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  ws,
  specs2 % Test,
  "org.scalatest"          %% "scalatest"    % "2.2.4" % "test",
  "joda-time"              %  "joda-time"    % "2.9.1",
  "com.github.dwhjames"    %% "aws-wrap"     % "0.8.0",
  "com.amazonaws"          %  "aws-java-sdk" % "1.10.34",
  "com.github.nscala-time" %% "nscala-time"  % "2.6.0",
  "com.github.etaty"       %% "rediscala"    % "1.5.0",
  "org.spire-math"         %% "cats"         % "0.3.0",
  "com.typesafe"           %  "config"       % "1.3.0"
)

fork := true
