package org.codersunit.tn.processing

import akka.actor._
import akka.routing.Broadcast
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import akka.routing.ConsistentHashingRouter
import scala.math.log
import org.codersunit.tn.output.limiter.Limiter

/** Counts how often received strings occur */
class Counter(what: String, limiter: Limiter, nChildren: Int = 0) extends Actor {
  /** Hashmap containing the counted strings */
  val counted = new HashMap[String, Int]()

  /** The children, if any, of this counter */
  var children: ActorRef = null

  /** The Actor that requested to be notified of the counting-results */
  var resultRequester: ActorRef = null

  /** Number of received results from childen */
  var received = 0

  /** Number of strings processed in this counter */
  var processed = 0

  // nChildren > 1 indicates that counting should be split up over multiple counters, creating those here
  if (nChildren > 1) {
    children = context.actorOf(
      Props(new Counter(what, limiter)).withRouter(ConsistentHashingRouter(nChildren)),
      name="counterChildren"
    )
  }

  /** Receives messages for this actor */
  def receive = {
    case c: Count => countWhere(c)
    case Result => generateResult()
    case Counted(map: Map[String, Int], what: String) => receiveResult(map)
    case s: Store => store(s)
  }

  /** Determine who has to execute the counting request */
  def countWhere(c: Count) = {
    if (nChildren > 1) {
      // this counter has children, use the hashingrouter to spread counting over children evenly
      children.forward(c)
    } else {
      // no children to process the input, I'll do it myself
      count(c.str)
    }
  }

  /** Count a string in this counter's local hashmap, notify the sender of the counting request */
  def count(str: String) {
    if (!counted.contains(str)) {
      counted += str -> 0
    }
    counted(str) += 1
    processed += 1
    sender ! Done
  }

  /** This counter was asked to generate a result, if it has children it will have to request their hashmaps first */
  def generateResult() {
    resultRequester = sender
    if (nChildren < 2) {
      complete()
    } else {
      children ! Broadcast(Result)
    }
  }

 /** This counter has received a result from a child, add their hashmap to ours */
  def receiveResult(map: Map[String, Int]) {
    received += 1
    counted ++= map
    if (received == nChildren) {
      // all children have sent their results, we can now send our own result to the requester
      complete()
    }
  }

  /** Complete the request for getting the result */
  def complete() {
    resultRequester ! Counted(counted, what)
  }

  def store(s: Store) {
    if (nChildren < 2) {
      process(s.words, s.assocs, s.writer)
    } else {
      children ! Broadcast(s)
    }
  }

  def process(wc: Map[String, (Double, Int)], totalAssocs: Int, writer: ActorRef) = {
    val ac = new HashMap[String, (Double, Int)]()
    for ((assoc, num) <- counted) {
      val assocList = assoc.split("\\|").toList
      var wChance: Double = 1.0

      for (word <- assocList) {
        wChance *= wc(word)._1
      }

      val aChance: Double = num / (totalAssocs * 1.0)
      val pmi = log(aChance / wChance)
      val npmi = pmi / -log(aChance)
      if (limiter.allowed(assocList, npmi, num)) {
        ac += assoc -> (npmi, num)
      }

    }
    writer ! Output(ac)
  }
}
