package pease.support

import spock.lang.Specification

class IdentifierConverterSpec extends Specification {
  def 'parameter names should be normalized'(String name, String normalized) {

    expect:
    IdentifierConverter.instance.toIdentifier(name) == normalized

    where:
    name                          | normalized
    'legalName'                   | 'legalName'
    'Illegal'                     | 'illegal'
    'ClassName'                   | 'className'
    'a space'                     | 'aSpace'
    'somename (with parenthesis)' | 'somenameWithParenthesis'
    'a thing with <placeholder>'  | 'aThingWithPlaceholder'
    'someillegal ©éäáöß€'         | 'someillegal_éäáöß€'
    '1 example'                   | 'a1Example'
  }
}
