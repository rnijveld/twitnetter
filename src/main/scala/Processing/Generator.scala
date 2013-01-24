package org.codersunit.tn.processing

import akka.actor._
import org.codersunit.tn.helper.WordCounter
import org.codersunit.tn.helper.tokenizer.Tokenizer


/** Generates words and skipgrams based on received sentences */
class Generator(
    wordCounter: ActorRef,
    assocCounter: ActorRef,
    master: ActorRef,
    ignored: Set[String],
    tokenizer: Tokenizer)
  extends Actor {

  /** Number of received sentences */
  var received = 0

  /** Number of sent words */
  var sentWords = 0

  /** Number of sent associations */
  var sentAssocs = 0

  /** Number of counted words and associations */
  var completed = 0

  /** Receive messages and send them to the correct processer */
  def receive = {
    case Sentence(str) => generate(str)
    case Done => complete()
  }

  /** Generate the words and skipgrams for a sentence. */
  def generate(sentence: String) {
    received += 1
    val tokens = tokenizer.tokenize(sentence)

    // send words, as long as they aren't being ignored
    for (word <- WordCounter.words(tokens)) {
      if (!ignored(word)) {
        wordCounter ! Count(word, self)
        sentWords += 1
      }
    }

    // send skipgrams as long as they don't contain an ignored word
    for (assoc <- WordCounter.skipgrams(tokens, 3, 2)) {
      if (assoc.size > 0 && assoc.forall(!ignored(_))) {
        assocCounter ! Count(assoc.reduce(_ + "|" + _), self)
        sentAssocs += 1
      }
    }
  }

  /** Receive a notification that counting was completed, notifies master when finished. */
  def complete() {
    completed += 1

    // all sent items are processed, notify the master that we are done
    if (completed == (sentWords + sentAssocs)) {
      master ! Completed(received, sentWords, sentAssocs)
      received = 0
      sentWords = 0
      sentAssocs = 0
      completed = 0
    }
  }
}
