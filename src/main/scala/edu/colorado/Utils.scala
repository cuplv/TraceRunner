package edu.colorado

/**
 * Created by s on 9/30/16.
 */
object Utils {
  def globToRegex(glob: String): String = {
    glob.flatMap((a: Char) =>
      a match {
        case '*' => ".*"
        case '?' => "."
        case '.' => "\\."
        case '\\' => "\\\\"
        case a => List(a)
      }
    )
  }
  def packageGlobToSignatureMatchingRegex(glob: String): String = {
    "<" + globToRegex(glob)
  }

}
