package pease.gherkin.model

import groovy.transform.InheritConstructors

@InheritConstructors
class Tags extends HashSet<String> {
  private static final String NEGATION_CHAR = '~'

  boolean matches(String tagExpression) {
    tagExpression == '' ? true : matchTagExpression(tagExpression)
  }

  boolean matches(List<String> expressions) {
    and(expressions.collect { matches it })
  }

  boolean matches(Tags tags) {
    and(tags.collect { matches it } )
  }

  private matchTagExpression(String tagExpression) {
    or tagExpression.split(',').collect { tag ->
      if (tooShort(tag)) {
        return false
      }

      if (tag.startsWith(NEGATION_CHAR)) {
        !contains(tagWithoutNegation(tag))
      } else {
        contains(tag)
      }
    }
  }

  private boolean tooShort(String tag) {
    tag.size() < 2
  }

  private String tagWithoutNegation(String tag) {
    def tokens = tag.split(NEGATION_CHAR)
    tokens.size() > 1 ? tokens[1] : ''
  }

  boolean and(List<Boolean> booleans) {
    booleans.inject(true) { result, value ->
      result && value
    }
  }

  private boolean or(List<Boolean> booleans) {
    booleans.inject(false) { result, value ->
      result || value
    }
  }
}
