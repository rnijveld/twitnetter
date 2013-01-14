import org.tartarus.snowball.ext.DutchStemmer
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import scala.io.Source
import org.apache.commons.lang3.StringEscapeUtils.unescapeJava
import akka.actor._
import akka.routing.RoundRobinRouter
import akka.routing.ConsistentHashingRouter
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import akka.routing.Broadcast
import scala.math.log

case class Sentence(str: String)
case object Run

case object Done
case class Completed(n: Int)

case class Count(str: String, respond: ActorRef) extends ConsistentHashable {
    def consistentHashKey = str
}

case object Result
case class Counted(map: Map[String, Int], what: String)

class Generator(wordCounter: ActorRef, assocCounter: ActorRef, master: ActorRef, ignoredWords: List[String]) extends Actor {
    var receivedSentences = 0
    var sentItems = 0
    var completedItems = 0

    def receive = {
        case Sentence(str) => {
            val tokens = Tokenizer.tokenize(str)
            receivedSentences += 1

            for(word <- WordCounter.words(tokens)) {
                if (!ignoredWords.contains(word)) {
                    wordCounter ! Count(word, self)
                    sentItems += 1
                }
            }

            for(assoc <- WordCounter.skipgrams(tokens, 3, 2)) {
                if (assoc.size > 0 && assoc.forall { !ignoredWords.contains(_) }) {
                    assocCounter ! Count(assoc.reduce(_+"|"+_), self)
                    sentItems += 1
                }
            }
        }
        case Done => {
            completedItems += 1
            if (completedItems == sentItems) {
                master ! Completed(receivedSentences)
                receivedSentences = 0
                sentItems = 0
                completedItems = 0
            }
        }
    }
}

class Counter(what: String, nChildren: Int = 0) extends Actor {
    var children: ActorRef = null
    if (nChildren > 0) {
        children = context.actorOf(
            Props(new Counter(what)).withRouter(ConsistentHashingRouter(nChildren)),
            name="counterChildren"
        )
    }

    val counted = new HashMap[String, Int]()
    var resultRequester: ActorRef = null
    var receivedResults = 0
    var processed = 0

    def receive = {
        case c: Count => {
            if (nChildren == 0) {
                if (!counted.contains(c.str)) {
                    counted += c.str -> 0
                }
                counted(c.str) += 1
                processed += 1
                c.respond ! Done
            } else {
                children ! c
            }
        }
        case Result => {
            resultRequester = sender
            if (nChildren == 0) {
                complete
            } else {
                children ! Broadcast(Result)
            }
        }
        case Counted(map: Map[String, Int], what: String) => {
            receivedResults += 1
            counted ++= map
            if (receivedResults == nChildren) {
                complete
            }
        }
    }

    def complete = {
        // Console.println("Sending counted back to " + resultRequester + ", I have " + nChildren + " children!")
        resultRequester ! Counted(counted, what)
    }
}

class Master(nrOfGenerators: Int, nrOfWordCounters: Int, nrOfAssocCounters: Int, input: String, ignoredWords: List[String]) extends Actor {
    val wordCounter = context.actorOf(Props(new Counter("words", nrOfWordCounters)), name="wordCounter")
    val assocCounter = context.actorOf(Props(new Counter("assocs", nrOfAssocCounters)), name="assocCounter")
    val generators = context.actorOf(
        Props(new Generator(wordCounter, assocCounter, self, ignoredWords)).withRouter(RoundRobinRouter(nrOfGenerators)),
        name="generators"
    )

    var sentLines = 0
    var completedLines = 0
    var completed = false

    var words: Option[Map[String, Int]] = None
    var assocs: Option[Map[String, Int]] = None

    def receive = {
        case Run => {
            for (line <- Source.fromFile(input).getLines()) {
                generators ! Sentence(unescapeJava(line))
                sentLines += 1
                // if (sentLines % 500 == 0) {
                //     Console.println("Scheduled " + sentLines)
                // }
            }
            completed = true
        }
        case Completed(n: Int) => {
            completedLines += n

            if (completed && completedLines == sentLines) {
                wordCounter ! Result
                assocCounter ! Result
            }
        }
        case Counted(map: Map[String, Int], what: String) => {
            // Console.println("Got " + map.size + " " + what + ", displaying top 50:")
            // for ((k,v) <- map.toList.sortWith{_._2 > _._2}.slice(0, 50) ) {
            //     Console.println(k+ ": " + v)
            // }
            // Console.println("Got " + assocs.size + " associations...")
            //
            if (what == "words") {
                words = Some(map)
            } else {
                assocs = Some(map)
            }

            words match {
                case Some(w: Map[String, Int]) => {
                    assocs match {
                        case Some(a: Map[String, Int]) => {
                            process(w, a)
                        }
                        case _ => {}
                    }
                }
                case _ => {}
            }
        }
    }

    def process(w: Map[String, Int], a: Map[String, Int]) = {
        val wTotal = w.foldLeft(0)(_+_._2)
        Console.println(wTotal + " of words")
        val wc = new HashMap[String, (Double, Int)]()
        for ((word, num) <- w) {
            wc += word -> (num / (wTotal * 1.0), num)
        }

        val aTotal = a.foldLeft(0)(_+_._2)
        Console.println(aTotal + " of assocs")
        val ac = new HashMap[String, (Double, Int)]()
        for ((assoc, num) <- a) {
            val assocList = assoc.split("\\|")
            var wChance: Double = 1.0

            for (word <- assocList) {
                wChance *= wc(word)._1
            }

            val aChance: Double = num / (aTotal * 1.0)
            val pmi = log(aChance / wChance)
            val npmi = pmi / -log(aChance)
            ac += assoc -> (npmi, num)
        }

        Console.println("Done")
        // for ((k,v) <- ac.toList.filter{_._2._2 >= 10}.sortWith{_._2._2 > _._2._2}.slice(0, 150) ) {
        //     Console.println(k + ": " + v)
        // }
        Grapher.gw(wc, ac, "vvd")
        context.system.shutdown()
    }
}

object Application extends App {
    val nrOfGenerators = 4
    val nrOfWordCounters = 5
    val nrOfAssocCounters = 25
    val input = "tweets2.dat"
    val ignoredWords = List("rt")

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
