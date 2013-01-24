package org.codersunit.tn.processing

import akka.actor._
import scala.collection.mutable.Map
import akka.routing.ConsistentHashingRouter.ConsistentHashable

/** All Messages should be serializable so that they can be sent over the network */
sealed class Message extends Serializable

/** Sentence to be processed */
case class Sentence(str: String) extends Message

/** Message indicating that counting is complete */
case object Done extends Message

/** Message indicating the number of sentences a generated has processed and counted */
case class Completed(n: Int, words: Int, assocs: Int) extends Message

/** Indicates that a given string should be counted and to what generated a response should be sent */
case class Count(str: String) extends Message with ConsistentHashable {
    def consistentHashKey = str
}

/** Request the counting results from a counter */
case object Result extends Message

/** The map containing counted results */
case class Counted(map: Map[String, Int], what: String) extends Message

/** Message indicating that all input was sent */
case object Finished extends Message

/** Send a request to a counter to use the given word-probabilities to generate output to be sent to the writer */
case class Store(words: Map[String, (Double, Int)], assocs: Int, writer: ActorRef) extends Message

/** Output that should be processed by the writer */
case class Output(assocs: Map[String, (Double, Int)]) extends Message
