import twitter4j._
import scala.io.Source
import java.lang.System
import org.apache.commons.lang3.StringEscapeUtils.escapeJava
import collection.JavaConversions._
import scala.collection.mutable.ArrayBuilder
import scala.collection

class Grabber(val storage:String = "tweets.dat") extends StatusListener {
    var listeners: List[(String, Option[Any] => Unit)] = Nil
    var startingTime = System.nanoTime()

    val out = Source.fromFile(storage)

    var grabbedTweets = 0

    val factory = new TwitterStreamFactory()
    val stream = factory.getInstance()
    stream.addListener(this)

    val query = new ArrayBuilder.ofRef[String]()

    listen("tweet", (tweet: Option[Any]) => tweet match {
        case Some(status: Status) => {
            out.++("%s\t%s\t%s\t%s\n".format(
                status.getId(),
                status.getUser().getId(),
                status.getCreatedAt(),
                escapeJava(status.getText())
            ))


            grabbedTweets += 1
            val runningMinutes = (System.nanoTime() - startingTime) / 60000000000.0
            val tpm = grabbedTweets / runningMinutes
            Console.print("\rGrabbed %d tweets in %.0f minutes (%.0f per minute)...".format(grabbedTweets, runningMinutes, tpm))
        }
        case _ => {}
    })

    def listen(event: String, listener: Option[Any] => Unit) = listeners ::= (event, listener)

    def trigger(event: String, data: Option[Any]) = {
        for ((e, l) <- listeners if e == event) {
            l(data)
        }
    }

    def start() = {
        startingTime = System.nanoTime()

        val queryResult: Array[String] = query.result()
        if (queryResult.length > 0) {
            val filter = new FilterQuery()
            filter.track(queryResult)
            stream.filter(filter)
        } else {
            stream.sample()
        }
    }

    def filter(keyword: Any) = keyword match {
        case k: String => query += k
        case ks: Array[String] => query ++= ks.toSeq
        case _ => throw new Exception("Unexpected argument type")
    }

    def onStatus(status: Status) = {
        trigger("tweet", Some(status))
    }

    def onDeletionNotice(notice: StatusDeletionNotice) = {
        trigger("delete", Some(notice))
    }

    def onTrackLimitationNotice(n: Int) = {
        trigger("limit", Some(n))
    }

    def onScrubGeo(user: Long, id: Long) = {
        trigger("scrubgeo", Some((user, id)))
    }

    def onException(ex: Exception) = {
        trigger("exception", Some(ex))
    }

}
