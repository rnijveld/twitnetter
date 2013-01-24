package org.codersunit.tn.output.formatter

object Csv extends Formatter {
    def format(assoc: List[String], prob: Double, count: Int) = {
        val assocs = assoc.reduce(_+"|"+_)
        s""""${assocs}","${prob}","${count}"\n"""
    }
}
