package org.codersunit.tn.output.limiter

case class Chain(children: Limiter*) extends Limiter {
    def allowed(assoc: List[String], prob: Double, count: Int): Boolean = {
        for (child <- children) {
            if (!child.allowed(assoc, prob, count)) {
                return false
            }
        }
        return true
    }
}
