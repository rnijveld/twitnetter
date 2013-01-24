package org.codersunit.tn.input

import twitter4j._
import scala.collection.mutable.ArrayBuilder
import collection.JavaConversions._
import scalax.file.Path
import scalax.io.StandardOpenOption.WriteAppend
import scalax.io._
import org.apache.commons.lang3.StringEscapeUtils.escapeJava

/** Input that provides strings by reading a file */
class Twitter(nTweets: Int) extends Input {
    var listeners: List[(String, Option[Any] => Unit)] = Nil
    val factory = new TwitterStreamFactory()
    val stream = factory.getInstance()
    stream.addListener(Listener)

    val query = new ArrayBuilder.ofRef[String]()
    var lang: List[String] = Nil
    var geos: List[Array[Double]] = Nil
    var grabbedTweets = 0

    var backup: Option[Output] = None

    listen("tweet", (tweet: Option[Any]) => tweet match {
        case Some(status: Status) => {
            if (lang.length <= 0 || lang.contains(status.getUser.getLang)) {
                backup match {
                    case Some(x: Output) => {
                        x.write(escapeJava(status.getText()) + "\n")
                    }
                    case _ => {}
                }
                next(status.getText())
                grabbedTweets += 1
                Console.print(s"\rReceived ${grabbedTweets} tweets...")
                if (grabbedTweets == nTweets) {
                    Console.println("")
                    stream.shutdown()
                    finish
                }
            }
        }
        case _ => {}
    })

    def run = {
        val queryResult: Array[String] = query.result()

        if (queryResult.length > 0 || geos.length > 0) {
            val filter = new FilterQuery()
            if (queryResult.length > 0) {
                filter.track(queryResult)
            }

            if (geos.length > 0) {
                val g: Array[Array[Double]] = geos.toArray
                filter.locations(g)
            }

            stream.filter(filter)
        } else {
            stream.sample()
        }
    }

    def filterGeo(southLat: Double, westLong: Double, northLat: Double, eastLong: Double) = {
        geos = geos ++ List(Array(westLong, southLat), Array(eastLong, northLat))
    }

    def filterLang(l: String) = lang ::= l

    def filter(keyword: Any) = keyword match {
        case k: String => query += k
        case ks: Array[String] => query ++= ks.toSeq
        case _ => throw new Exception("Unexpected argument type")
    }

    def backupTo(file: String) = {
        backup = Some(Path(file).outputStream(WriteAppend:_*))
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

        def onStallWarning(warning: StallWarning) = {
            trigger("stalled", Some(warning))
        }
    }

    def listen(event: String, listener: Option[Any] => Unit) = listeners ::= (event, listener)

    def trigger(event: String, data: Option[Any]) = {
        for ((e, l) <- listeners if e == event) {
            l(data)
        }
    }
}
