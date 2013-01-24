package org.codersunit.tn.output.limiter

trait Limiter {
    def allowed(assoc: List[String], prob: Double, count: Int): Boolean
}
