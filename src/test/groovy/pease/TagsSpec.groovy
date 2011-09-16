package pease

import pease.gherkin.model.Tags
import spock.lang.Specification

@SuppressWarnings('DuplicateStringLiteral')
class TagsSpec extends Specification {
  def tags = new Tags()

  def 'it should not be possible to have two instances of the same tag-string in the same Tags object'() {
    when:
    tags << '@mytag'
    tags << '@mytag'

    then:
    tags.size() == 1
  }

  def 'tag matching should work as expected'() {
    given:
    tags.addAll(initialTags)

    expect:
    tags.matches(matcher as String) == result

    where:
    initialTags              | matcher  | result
    ['@mytag']               | '@mytag' | true
    ['@atag', '@anothertag'] | '@atag'  | true
    ['@foo']                 | '~@bar'  | true
    ['@a', '@b']             | '@c,@b'  | true
    ['@a', '@b']             | ''       | true

    ['@foo']                 | '~@foo'  | false
    ['@a', '@b']             | '@d,@e'  | false

    ['@a', '@b']             | '~'      | false
  }

  def 'a list of tag expressions should be treated as if combined with an AND'() {
    given:
    tags.addAll(initialTags)

    expect:
    tags.matches(expressions as List) == result

    where:
    initialTags  | expressions   | result
    ['@a', '@b'] | ['@b', '@a']  | true  // B and A
    ['@a']       | ['@a', '@b']  | false // A and B
    ['@a']       | ['@a', '~@a'] | false // A and ~A
    ['@b', '@c'] | ['~@a', '@c'] | true  // ~A and C
  }
}