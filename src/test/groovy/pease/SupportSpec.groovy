package pease

import pease.support.Either
import pease.support.Either.Left
import pease.support.Either.Right
import pease.support.Maybe
import pease.support.Maybe.Just
import pease.support.Maybe.Nothing
import spock.lang.Specification

// Tests for helper/support classes
class SupportSpec extends Specification {
  final static MAGIC_NUMBER = 42
  final static ERROR_CONSTANT = 'failed'

  Maybe div(a, b) { b == 0 ? Nothing.instance : new Just(a / b) }

  def 'either works as expected'() {
    given:
    Either<String, Integer> result = new Right(MAGIC_NUMBER)
    Either<String, Integer> error = new Left(ERROR_CONSTANT)

    expect:
    result.class == Right
    result.right== MAGIC_NUMBER
    result.left == null

    and:
    error.class == Left
    error.left == ERROR_CONSTANT
    error.right == null
  }

  def 'maybe works as expected'() {
    given:
    def result = div(1,2)
    def nothing = div(5,0)

    and:
    def correctResult = 0.5

    expect:
    result.isJust()
    result.fromJust() == correctResult
    result.fromMaybe(23) == correctResult

    and:
    nothing.isNothing()
    nothing.fromMaybe(ERROR_CONSTANT) == ERROR_CONSTANT
  }
}