package pease.support

// source: https://groovyconsole.appspot.com/view.groovy?id=7006
abstract class Monad<T> {
  T value;

  Monad() {}

  Monad(T a) {
    value = a
  }

  // only ">>>" is possible to override ">>=" would have been the preferred solution
  abstract Monad<T> rightShiftUnsigned(Closure<Monad<T>> c)
}