package org.codersunit.tn

// import org.tartarus.snowball.ext.DutchStemmer
import akka.actor._
import org.codersunit.tn.processing.Master
import org.codersunit.tn.processing.{Associations, Words}
import org.codersunit.tn.input.Input
import org.codersunit.tn.input.{JavaEscapedFile, Twitter}
import org.codersunit.tn.helper.tokenizer


object Application extends App {
	val nrOfGenerators = 4
	val nrOfWordCounters = 5
	val nrOfAssocCounters = 25
	val ignoredWords = Set("rt")
	// val input = new JavaEscapedFile("tweets3.dat")
  val input = new Twitter(50)
  val tokenizerImpl = tokenizer.Twitter

	val system = ActorSystem("TwitNetter")
	val master = system.actorOf(Props(new Master(
		nrOfGenerators,
		nrOfWordCounters,
		nrOfAssocCounters,
		ignoredWords,
		tokenizerImpl
	)))
	input >> master
}
