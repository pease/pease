package pease.support

@SpockTestClass('IdentifierConverterSpec')
@Singleton
/** Converts a random string into an identifier representation. Does not guarantee uniqueness!  */
class IdentifierConverter {
  String firstLower(String s) {
    if (s.size() > 1) {
      s[0].toLowerCase() + s[1..-1]
    } else {
      s.toUpperCase()
    }
  }

  String replaceNonJavaLetters(String s) {
    s.collect { c ->
      (Character.isDigit((char) c) || Character.isJavaLetter((char) c)) ? c : '_'
    }.join('')
  }

  String removeInvalidChars(String s) {
    def removeChars = ['(', ')', '<', '>']

    removeChars.each {
      s = s.replace(it, '')
    }

    s
  }

  String prefixFirstDigit(String s) {
    (Character.isDigit((char) s[0]) ? 'a' : '') + s
  }

  List<String> upperCaseFirstLetters(List<String> tokens) {
    tokens.collect {
      if (it.size() > 1) {
      "${it[0].toUpperCase()}${it[1..-1]}"
      } else {
        it.toUpperCase()
      }
    }
  }

  String toIdentifier(String str) {
    List<String> tokens = (this.&upperCaseFirstLetters) removeInvalidChars(str).tokenize(' ')

    (this.&prefixFirstDigit << this.&firstLower << this.&replaceNonJavaLetters) tokens.join('')
  }
}
