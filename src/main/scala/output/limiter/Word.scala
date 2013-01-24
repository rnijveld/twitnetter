package org.codersunit.tn.output.limiter

case class Word(word: String) extends Limiter {
    def allowed(assoc: List[String], prob: Double, count: Int): Boolean = {
        !assoc.contains(word)
    }
}
