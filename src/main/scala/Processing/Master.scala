package org.codersunit.tn.processing

import akka.actor._
import akka.routing.RoundRobinRouter
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import org.codersunit.tn.input.Input
import org.codersunit.tn.helper.tokenizer.Tokenizer
import org.codersunit.tn.output.formatter.Formatter
import org.codersunit.tn.output.limiter.Limiter

/** Master that sends out all tasks for counting words */
class Master(
    nrOfGenerators: Int,
    nrOfWordCounters: Int,
    nrOfAssocCounters: Int,
    ignoredWords: Set[String],
    tokenizer: Tokenizer,
    output: String,
    formatter: Formatter,
    limiter: Limiter)
  extends Actor {

  /** Counter for words */
  val wordCounter = context.actorOf(Props(new Counter("words", limiter, nrOfWordCounters)), name="wordCounter")

  /** Counter for associations */
  val assocCounter = context.actorOf(Props(new Counter("assocs", limiter, nrOfAssocCounters)), name="assocCounter")

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

  def receive = {
    case s: Sentence => {
      sentLines += 1
      generators ! s
    }
    case Finished => {
      Console.println("Received all input, finishing processing...")
      completed = true
      if (completedLines == sentLines) {
        wordCounter ! Result
      }
    }
    case Completed(n: Int, words: Int, assocs: Int) => {
      completedLines += n
      totalWords += words
      totalAssocs += assocs

      if (completed && completedLines == sentLines) {
        wordCounter ! Result
      }
    }
    case Counted(map: Map[String, Int], what: String) => {
      Console.println(totalWords + " of words")
      Console.println(totalAssocs + " of assocs")
      Console.println("Calculating normalized PMI and saving results to file...")

      val wc = new HashMap[String, (Double, Int)]()

      // calculate individual chances of words
      for ((word, num) <- map) {
        wc += word -> (num / (totalWords * 1.0), num)
      }

      // request the assoccounter to use the word probabilities to generate PMI results and write them to the output
      val writer = context.actorOf(Props(new Writer(output, formatter, nrOfAssocCounters)), name="writer")
      assocCounter ! Store(wc, totalAssocs, writer)
    }
  }
}
