// import org.tartarus.snowball.ext.DutchStemmer
import akka.actor._

object Application extends App {
    val nrOfGenerators = 4
    val nrOfWordCounters = 5
    val nrOfAssocCounters = 25
    val input = "tweets2.dat"
    val ignoredWords = Set("rt")

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
