import akka.actor._
import akka.routing.RoundRobinRouter
import scala.math.log
import scala.collection.mutable.HashMap
import scala.io.Source
import org.apache.commons.lang3.StringEscapeUtils.unescapeJava

/** Master that sends out all tasks for counting words */
class Master(
    nrOfGenerators: Int,
    nrOfWordCounters: Int,
    nrOfAssocCounters: Int,
    input: String,
    ignoredWords: Set[String])
  extends Actor {

  /** Counter for words */
  val wordCounter = context.actorOf(Props(new Counter("words", nrOfWordCounters)), name="wordCounter")

  /** Counter for associations */
  val assocCounter = context.actorOf(Props(new Counter("assocs", nrOfAssocCounters)), name="assocCounter")

  /** Generator that generates words and associations for the counters */
  val generators = context.actorOf(
    Props(new Generator(wordCounter, assocCounter, self, ignoredWords)).withRouter(RoundRobinRouter(nrOfGenerators)),
    name="generators"
  )

  /** Number of lines sent off for processing */
  var sentLines = 0

  /** Number of lines for which processing was completed */
  var completedLines = 0

  /** Boolean indicating if all lines have been processed */
  var completed = false

  var words: Option[Map[String, Int]] = None
  var assocs: Option[Map[String, Int]] = None

  def receive = {
    case Run => {
      for (line <- Source.fromFile(input).getLines()) {
        generators ! Sentence(unescapeJava(line))
        sentLines += 1
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
    Grapher.gw(wc, ac, "vvd")
    context.system.shutdown()
  }
}
