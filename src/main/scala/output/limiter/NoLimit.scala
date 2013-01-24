package org.codersunit.tn.output.limiter

case object NoLimit extends Limiter {
    def allowed(assoc: List[String], prob: Double, count: Int) = true
}
