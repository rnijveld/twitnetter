seq(lsSettings :_*)

name := "TwitNetter"

scalaVersion := "2.10.0"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions += "-deprecation"

libraryDependencies += "org.twitter4j" % "twitter4j-core" % "3.0.3"

libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "3.0.3"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "org.apache.lucene" % "lucene-analyzers" % "3.6.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.0"

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2"

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"

// fork in run := true
