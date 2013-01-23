package org.codersunit.tn

// import org.tartarus.snowball.ext.DutchStemmer
import akka.actor._
import org.codersunit.tn.processing.Master
import org.codersunit.tn.processing.Run
import org.codersunit.tn.input.File

object Application extends App {
    val nrOfGenerators = 4
    val nrOfWordCounters = 5
    val nrOfAssocCounters = 25
    val ignoredWords = Set("rt")

    val input = new File("tweets3.dat")

    val system = ActorSystem("TwitNetter")
    val master = system.actorOf(Props(new Master(
        nrOfGenerators,
        nrOfWordCounters,
        nrOfAssocCounters,
        input,
        ignoredWords
    )))
    master ! Run

}
