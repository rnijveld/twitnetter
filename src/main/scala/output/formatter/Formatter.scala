package org.codersunit.tn.output.formatter

trait Formatter {
    def format(assoc: List[String], prob: Double, count: Int): String
}
