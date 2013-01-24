package org.codersunit.tn

// import org.tartarus.snowball.ext.DutchStemmer
import akka.actor._
import org.codersunit.tn.processing.Master
import org.codersunit.tn.input.Input
import org.codersunit.tn.input.{File, JavaEscapedFile, Twitter}
import org.codersunit.tn.helper.tokenizer
import org.codersunit.tn.output.formatter
import org.codersunit.tn.output.limiter._


object Application extends App {
  // number of generators that tokenizer input
	val nrOfGenerators = 4

  // number of workers for counting words
	val nrOfWordCounters = 5

  // number of workers for counting associations
	val nrOfAssocCounters = 25

  // these words will be ignored, as well as any association that contains one or more of these words
	val ignoredWords = Set("rt")

  // specify the input reader
  val input = new File("input.dat")
	// val input = new JavaEscapedFile("tweets3.dat")
  //val input = new Twitter(10000)
  // input.filterLang("nl")
  //input.filterGeo(50.76, 3.34, 53.56, 7.20)

  // what tokenizer to use on the input
  val tokenizerImpl = tokenizer.Twitter

  // limit the outputted results to those confirming to these constraints
  val limiter = Chain(Count(20))

  // file where output should be stored
  val output = "output.csv"

  // file output format
  val format = formatter.Csv

  // allright, we've got everything, creating the actorsystem and master actor
	val system = ActorSystem("TwitNetter")
	val master = system.actorOf(Props(new Master(
		nrOfGenerators,
		nrOfWordCounters,
		nrOfAssocCounters,
		ignoredWords,
		tokenizerImpl,
    output,
    format,
    limiter
	)))

  // ask the input provider to send its input to the master
	input >> master
}
