package org.codersunit.tn.input

import org.apache.commons.lang3.StringEscapeUtils.unescapeJava
import java.io.FileReader
import java.io.BufferedReader
import java.io.{File => JavaFile}
import java.io.FileDescriptor

/** Input reader that can read from files where the individual lines are escaped java-strings */
class JavaEscapedFile(reader: BufferedReader) extends Input {
    /** Create a new file input using a FileReader */
    def this(reader: FileReader) = this(new BufferedReader(reader))

    /** Create a new file input using a filename */
    def this(file: String) = this(new FileReader(file))

    /** Create a new file input using java.io.File */
    def this(file: JavaFile) = this(new FileReader(file))

    /** Create a new file input using java.io.FileDescriptor */
    def this(file: FileDescriptor) = this(new FileReader(file))

    def run = {
        var line: String = null
        do {
            line = unescapeJava(reader.readLine())
            if (line != null) {
                next(line)
            }
        } while (line != null)
        finish
    }
}
