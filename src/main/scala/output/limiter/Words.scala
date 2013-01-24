package org.codersunit.tn.output.limiter

case class Words(words: String*) extends Limiter {
    def allowed(assoc: List[String], prob: Double, count: Int): Boolean = {
        for (w <- words) {
            if (assoc.contains(w)) {
                return false
            }
        }
        return true
    }
}
