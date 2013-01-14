seq(lsSettings :_*)

name := "TwitNetter"

scalaVersion := "2.10.0-RC5"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions += "-deprecation"

libraryDependencies += "org.twitter4j" % "twitter4j-core" % "2.2.6"

libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "2.2.6"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "org.apache.lucene" % "lucene-analyzers" % "3.6.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.0-RC6" cross CrossVersion.full

// fork in run := true
