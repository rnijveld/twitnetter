package org.codersunit.tn.processing

import akka.actor._
import akka.routing.RoundRobinRouter
import scala.math.log
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import org.codersunit.tn.input.Input
import org.codersunit.tn.helper.tokenizer.Tokenizer

/** Master that sends out all tasks for counting words */
class Master(
    nrOfGenerators: Int,
    nrOfWordCounters: Int,
    nrOfAssocCounters: Int,
    ignoredWords: Set[String],
    tokenizer: Tokenizer)
  extends Actor {

  /** Counter for words */
  val wordCounter = context.actorOf(Props(new Counter("words", nrOfWordCounters)), name="wordCounter")

  /** Counter for associations */
  val assocCounter = context.actorOf(Props(new Counter("assocs", nrOfAssocCounters)), name="assocCounter")

  /** Generator that generates words and associations for the counters */
  val generators = context.actorOf(
    Props(new Generator(wordCounter, assocCounter, self, ignoredWords, tokenizer)).withRouter(RoundRobinRouter(nrOfGenerators)),
    name="generators"
  )

  /** Number of lines sent off for processing */
  var sentLines = 0

  /** Number of lines for which processing was completed */
  var completedLines = 0

  /** Number of words counted */
  var totalWords = 0

  /** Number of assocations counted */
  var totalAssocs = 0

  /** Boolean indicating if all lines have been processed */
  var completed = false

  var words: Option[Map[String, Int]] = None
  var assocs: Option[Map[String, Int]] = None

  def receive = {
    case s: Sentence => {
      sentLines += 1
      generators ! s
    }
    case Finished => completed = true
    case Completed(n: Int, words: Int, assocs: Int) => {
      completedLines += n
      totalWords += words
      totalAssocs += assocs

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
    Console.println(totalWords + " of words")
    val wc = new HashMap[String, (Double, Int)]()
    for ((word, num) <- w) {
      wc += word -> (num / (totalWords * 1.0), num)
    }

    Console.println(totalAssocs + " of assocs")
    val ac = new HashMap[String, (Double, Int)]()
    for ((assoc, num) <- a) {
      val assocList = assoc.split("\\|")
      var wChance: Double = 1.0

      for (word <- assocList) {
        wChance *= wc(word)._1
      }

      val aChance: Double = num / (totalAssocs * 1.0)
      val pmi = log(aChance / wChance)
      val npmi = pmi / -log(aChance)
      ac += assoc -> (npmi, num)
    }

    Console.println("Done")
    // Grapher.gw(wc, ac, "vvd")
    context.system.shutdown()
  }
}
