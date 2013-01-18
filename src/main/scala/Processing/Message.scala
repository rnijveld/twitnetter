import akka.actor._
import scala.collection.mutable.Map
import akka.routing.ConsistentHashingRouter.ConsistentHashable

/** All Messages should be serializable so that they can be sent over the network */
sealed class Message extends Serializable

/** Sentence to be processed */
case class Sentence(str: String) extends Message

/** Message indicating that the master should start processing */
case object Run extends Message

/** Message indicating that counting is complete */
case object Done extends Message

/** Message indicating the number of sentences a generated has processed and counted */
case class Completed(n: Int) extends Message

/** Indicates that a given string should be counted and to what generated a response should be sent */
case class Count(str: String, respond: ActorRef) extends Message with ConsistentHashable {
    def consistentHashKey = str
}

/** Request the counting results from a counter */
case object Result extends Message

/** The map containing counted results */
case class Counted(map: Map[String, Int], what: String) extends Message
