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

  // if we're going for 1 child, then we might as well use this counter for efficiency
  if (nChildren == 1) {
    nChildren = 0
  }

  // nChildren > 0 indicates that counting should be split up over multiple counters, creating those here
  if (nChildren > 0) {
    children = context.actorOf(
      Props(new Counter(what)).withRouter(ConsistentHashingRouter(nChildren)),
      name="counterChildren"
    )
  }

  /** Receives messages for this actor */
  def receive = {
    case c: Count => {
      if (nChildren > 0) {
        // this counter has children, use the hashingrouter to spread results over children evenly
        children ! c
      } else {
        // no children to process the input, I'll do it myself
        count(c.str, c.respond)
      }
    }
    case Result => generateResult()
    case Counted(map: Map[String, Int], what: String) => receiveResult()
  }

  /** Count a string in this counter's local hashmap, notify the sender of the counting request */
  def count(str: String, parent: ActorRef) {
    if (!counted(str)) {
      counted += str -> 0
    }
    counted(str) += 1
    processed += 1
    parent ! Done
  }

  /** This counter was asked to generate a result, if it has children it will have to request their hashmaps first */
  def generateResult() {
    resultRequester = sender
    if (nChildren == 0) {
      complete()
    } else {
      children ! Broadcast(Result)
    }
  }

 /** This counter has received a result from a child, add their hashmap to ours */
  def receiveResult() {
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
}
