package org.codersunit.tn.processing

import akka.actor._
import org.codersunit.tn.output.formatter.Formatter
import scala.collection.mutable.Map
import scalax.file.Path
import scalax.io.StandardOpenOption.WriteAppend
import scalax.io._
import processing.Processor

class Writer(file: String, formatter: Formatter, expected: Int) extends Actor {
  var received = 0

  val path = Path(file)
  path.deleteIfExists()
  val output = path.outputStream(WriteAppend:_*)

  def receive = {
    case Output(map: Map[String, (Double, Int)]) => {
      received += 1

      for ((a, (prob, num)) <- map) {
        val assoc = a.split("\\|").toList
        val str = formatter.format(assoc, prob, num)
        output.write(str)(Codec.UTF8)
      }

      if (received == expected) {
        Console.println("Done")
        context.system.shutdown()
      }
    }
  }
}
