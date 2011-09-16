package pease.support

import groovy.transform.TupleConstructor

// Inspired from Haskell/Scala
// From the Data.Either haskell docs: `mnemonic: "right" also means "correct""
@TupleConstructor
@SuppressWarnings(['AbstractClassWithoutAbstractMethod','EmptyMethodInAbstractClass'])
abstract class Either<A,B> {
  private Either() {}

  A getLeft() { null }
  B getRight() { null }

  @TupleConstructor
  final static class Left<A, B> extends Either<A, B> {
    A left

    A getLeft() { left}
  }

  @TupleConstructor
  final static class Right<A, B> extends Either<A, B> {
    B right

    B getRight() { right }
  }
}
