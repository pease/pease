package pease.support

import pease.support.Maybe.Just
import pease.support.Maybe.Nothing
import spock.lang.Specification

class MaybeSpec extends Specification {
  def 'it should be possible to test whats inside the maybe'(Maybe<Integer> m, boolean result) {
    expect:
    m.isNothing() == result

    where:
    m                | result
    new Just(42)     | false
    Nothing.instance | true
  }

  def 'Nothing should be a singleton object'() {
    expect:
    Nothing.instance == Nothing.instance
  }

  def 'maybe should be a monad'() {
    given:
    def m1 = new Just(5)

    when:
    def result = m1 >>> { n -> new Just(n * 2) }

    then:
    result.isJust()

    and:
    result.fromJust() == 10
  }

  def 'the maybe monad should behave naughty and do side effected operations'() {
    given:
    def a = 5

    and:
    def m = new Just(5)

    when:
    def result = m >>> { n -> a = 23; Nothing.instance }

    then:
    a == 23

    and:
    result.isNothing()
  }
}
