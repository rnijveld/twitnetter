package org.codersunit.tn

import scala.collection.mutable.Map
import scala.collection.mutable.StringBuilder
import java.io.FileWriter

object Grapher {
    def graph(words: Map[String, (Double, Int)], assocs: Map[String, (Double, Int)], outputFile: String = "output.dot") = {
        var output = new StringBuilder()
        output append "graph G {\n"

        var availableNodes: List[String] = Nil
        for ((word, (prob, count)) <- words if count > 500) {
            availableNodes = word :: availableNodes

            output append word
            output append " [shape=circle,penwidth="
            // determine pen width
            output append (((1.0 - (1.0 / count)) * 4) + 1).toInt
            output append "] ;\n"
        }

        output append "\n\n\n"

        for ((assoc, (prob, count)) <- assocs) {
            val a = assoc.split("\\|")
            if (a.forall{availableNodes contains _}) {
                output append a(0)
                output append " -- "
                output append a(1)
                output append " [weight="
                output append (((prob + 1.0) / 2.0 + 1) * 1)
                output append "] ;\n"
            }
        }

        output ++= "}"
        val outputString = output.result()

        val writer = new FileWriter(outputFile)
        writer.write(outputString)
        writer.close
    }

    def gw(words: Map[String, (Double, Int)], assocs: Map[String, (Double, Int)], center: String, outputFile: String = "output.dot") = {
        val output = new StringBuilder()
        output append "graph G {\n"

        var n = 0
        for ((assoc, (prob, count)) <- assocs.toList.sortWith{_._2._1 > _._2._1} if n < 20) {
            val a = assoc.split("\\|")
            if (a.contains(center)) {
                output append a(0)
                output append " -- "
                output append a(1)
                output append " [weight="
                output append (((prob + 1.0) / 2.0 + 1) * 1)
                output append "] ;\n"
                n += 1
            }
        }

        output ++= "}"
        val outputString = output.result()

        val writer = new FileWriter(outputFile)
        writer.write(outputString)
        writer.close
    }
}
