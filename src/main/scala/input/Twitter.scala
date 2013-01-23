package org.codersunit.tn.input

import twitter4j._
import scala.collection.mutable.ArrayBuilder

/** Input that provides strings by reading a file */
class Twitter(nTweets: Int) extends Input {
    val factory = new TwitterStreamFactory()
    val stream = factory.getInstance()
    stream.addListener(Listener)

    val query = new ArrayBuilder.ofRef[String]()
    var grabbedTweets = 0

    listen("tweet", (tweet: Option[Any]) => tweet match {
        case Some(status: Status) => {
            next(status.getText())
            grabbedTweets += 1
            if (grabbedTweets == nTweets) {
                finish
            }
        }
        case _ => {}
    })

    def run = {
        val queryResult: Array[String] = query.result()
        if (queryResult.length > 0) {
            val filter = new FilterQuery()
            filter.track(queryResult)
            stream.filter(filter)
        } else {
            stream.sample()
        }
    }

    object Listener extends StatusListener {
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

    var listeners: List[(String, Option[Any] => Unit)] = Nil

    def listen(event: String, listener: Option[Any] => Unit) = listeners ::= (event, listener)

    def trigger(event: String, data: Option[Any]) = {
        for ((e, l) <- listeners if e == event) {
            l(data)
        }
    }
}
