package org.codersunit.tn.input

import scala.util.parsing.input.Reader

/** Abstract input class */
abstract class Input extends Reader[String] {
    /** Current line number */
    def line: Int

    def rest: Input

    /** Takes the line number and current string and generates a Position from it */
    def pos = Position(line, first)
}
