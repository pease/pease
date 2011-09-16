package pease.support

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@TupleConstructor(includeFields = true)
@EqualsAndHashCode
class Tuple<A, B> {
  private A a
  private B b

  A fst() { a }

  B snd() { b }

  List toList() { [a, b] }
}
