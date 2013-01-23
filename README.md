# Word association counter
This word association counter can be used to generate a network of associations for any input consisting of sentences. This packages provides an interface for providing input from any source requested, as long as the input is provided in the form of sentences.

## Requirements
- Scala 2.10.0 or higher
    - Java 6 or 7
- Scala build tool

Scala can be downloaded from http://www.scala-lang.org. The scala build tool can be downloaded from http://www.scala-sbt.org. Both scala and sbt provide Windows installers that will automatically add those tools to the PATH environment variables. Just open up cmd and you're ready to go. For Mac, I suggest using macports from http://www.macports.org. After installing it you can install scala and sbt by running

    sudo port selfupdate && sudo port install scala2.10 sbt

If you're a Linux user you may check if your distribution provides scala packages (scala may not be up to date!), otherwise you'll have to install them manually.

## Running it
If you're not using it in a cluster, a simple local instance can be started by running this command in any terminal:

    sbt run

## Input types
By default the word association counter supports counting from a file (either just plain text or text escaped
with java escape sequences) and getting a fixed number of tweets from Twitter given some query. While results flow
in from Twitter, they are directly handled by the system as to not create a backlog. Take a look at the classes that
are used for generating the input in `src/main/scala/input`, especially the `Input` abstract class which should be
extended if different types of input are required.

## Tokenizers
Provided is a tokenizer for tweets, that removes @handle references, urls and other special characters, furthermore
it transforms all input to lowercase. If other Tokenizers are required, you'll want to extend the `Tokenizer` class
in `src/main/scala/helper/tokenizer`.


