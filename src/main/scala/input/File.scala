package org.codersunit.tn.input

import java.io.FileReader
import java.io.BufferedReader
import java.io.{File => JavaFile}
import java.io.FileDescriptor

/** Input that provides strings by reading a file */
case class File(reader: BufferedReader, current: String, line: Int = 1) extends Input {
    /** Create a new file input using a BufferedReader */
    def this(reader: BufferedReader) = this(reader, reader.readLine())

    /** Create a new file input using a FileReader */
    def this(reader: FileReader) = this(new BufferedReader(reader))

    /** Create a new file input using a filename */
    def this(file: String) = this(new FileReader(file))

    /** Create a new file input using java.io.File */
    def this(file: JavaFile) = this(new FileReader(file))

    /** Create a new file input using java.io.FileDescriptor */
    def this(file: FileDescriptor) = this(new FileReader(file))

    val next = reader.readLine()

    // if we're done, we can close the reader
    if (atEnd) {
        reader.close()
    }

    /** If we don't have any line, after the current one, we can stop reading */
    def atEnd = next == null

    /** Retrieve the current line */
    def first = current

    /** Go to the next line */
    def rest = File(reader, next, line + 1)
}
