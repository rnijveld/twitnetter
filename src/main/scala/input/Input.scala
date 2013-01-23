package org.codersunit.tn.input

import org.codersunit.tn.processing.{Finished, Sentence}
import akka.actor._

/** Abstract input class */
abstract class Input {
    var m: ActorRef = null

    /** Method to be called when no more input is to be expected */
    def finish = {
        m ! Finished
    }

    /** Method to be called when a new input is received and should be processed */
    def next(sentence: String) = {
        m ! Sentence(sentence)
    }

    /** Method that will be called when the Input class should start generating input */
    def run: Unit

    /** Send input to the actor that is responsible for assigning work */
    def >>(master: ActorRef) = {
        m = master
        run
    }
}
