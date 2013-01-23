package org.codersunit.tn.helper.tokenizer

import java.net.URL
import java.net.MalformedURLException
import scala.util.matching.Regex

object Twitter extends Tokenizer {
    def tokenize(str: String): List[String] = {
        str.split("""\s+""").toList.filterNot(
            (x: String) => isTwitterDetail(x) || isUrl(x)
        ).map(
            (x: String) => cleanToken(x)
        ).filter(
            (x: String) => x.length() > 0
        )
    }

    def isUrl(x: String): Boolean = {
        try {
            new URL(x)
            true
        } catch {
            case _: MalformedURLException => false
        }
    }

    def isTwitterDetail(x: String): Boolean = {
        x.length > 0 && x(0) == '@'
    }

    def cleanToken(x: String): String = {
        val regex = """[^\p{IsL}]""".r
        regex.replaceAllIn(x.toLowerCase(), "")
    }
}
