package org.codersunit.tn.processing

import akka.actor._
import akka.routing.Broadcast
import scala.collection.mutable.HashMap
import akka.routing.ConsistentHashingRouter

/** Counts how often received strings occur */
class Counter(what: String, nChildren: Int = 0) extends Actor {
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
      Props(new Counter(what)).withRouter(ConsistentHashingRouter(nChildren)),
      name="counterChildren"
    )
  }

  /** Receives messages for this actor */
  def receive = {
    case c: Count => countWhere(c)
    case Result => generateResult()
    case Counted(map: Map[String, Int], what: String) => receiveResult(map)
  }

  /** Determine who has to execute the counting request */
  def countWhere(c: Count) = {
    if (nChildren > 1) {
      // this counter has children, use the hashingrouter to spread counting over children evenly
      children ! c
    } else {
      // no children to process the input, I'll do it myself
      count(c.str, c.respond)
    }
  }

  /** Count a string in this counter's local hashmap, notify the sender of the counting request */
  def count(str: String, parent: ActorRef) {
    if (!counted.contains(str)) {
      counted += str -> 0
    }
    counted(str) += 1
    processed += 1
    parent ! Done
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
    Console.println(s"Received ${received} results, expecting ${nChildren}")
    if (received == nChildren) {
      // all children have sent their results, we can now send our own result to the requester
      complete()
    }
  }

  /** Complete the request for getting the result */
  def complete() {
    resultRequester ! Counted(counted, what)
  }
}
