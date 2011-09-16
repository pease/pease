package pease.support

import groovy.transform.TupleConstructor

@TupleConstructor
@SpockTestClass('MaybeSpec')
abstract class Maybe<A> extends Monad<A> {

  abstract boolean isJust()
  boolean isNothing() { !isJust() }
  abstract A fromJust()
  abstract A fromMaybe(A defaultValue)

  @Override
  Monad<A> rightShiftUnsigned(Closure<Monad<A>> c) {
    if (isJust()) {
      def a = fromJust()
      c.call(a)
    } else {
      Nothing.instance
    }
  }

  @TupleConstructor
  final static class Just<A> extends Maybe<A> {
    A value

    boolean isJust() { true }
    A fromJust() { value }
    A fromMaybe(A defaultValue) { value }
  }

  @TupleConstructor
  @Singleton
  final static class Nothing<A> extends Maybe<A> {
    boolean isJust() { false }
    A fromJust() { null }
    A fromMaybe(A defaultValue) { defaultValue }

    private Nothing() {
      Nothing.instance
    }
  }
}
