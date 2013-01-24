package org.codersunit.tn.output.limiter

case class Count(min: Int) extends Limiter {
    def allowed(assoc: List[String], prob: Double, count: Int): Boolean = {
        count >= min
    }
}
