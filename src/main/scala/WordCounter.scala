import scala.collection.immutable.HashSet

object WordCounter {
    def skipgrams(tokens: List[String], skip: Int, size: Int = 2): Set[List[String]] = {
        def skippart(tokens: List[String], skip: Int, size: Int, cskip: Int, bucket: List[String]): Set[List[String]] = {
            if (cskip > size) HashSet[List[String]]()
            else if (bucket.size == size) HashSet()+(bucket.sorted)
            else tokens match {
                case Nil => HashSet[List[String]]()
                case token :: rest => {
                    skippart(rest, skip, size, cskip + 1, bucket) ++ skippart(rest, skip, size, 1, token :: bucket)
                }
            }
        }

        tokens match {
            case Nil => HashSet[List[String]]()
            case token :: rest => {
                if (rest.length < size - 1) {
                    HashSet[List[String]]()
                } else {
                    skipgrams(rest, skip, size) ++ skippart(rest, skip, size, 1, List(token))
                }
            }
        }
    }

    def words(tokens: List[String]) = tokens.distinct
}
