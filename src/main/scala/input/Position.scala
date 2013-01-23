package org.codersunit.tn.input

/** Simple implementation for this use of the Position class, our tokens are lines and we're always at column 1 */
case class Position(line: Int, lineContents: String) extends scala.util.parsing.input.Position {
    def column = 1
}
