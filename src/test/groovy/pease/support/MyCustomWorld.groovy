package pease.support

import spock.lang.Shared
import spock.lang.Specification

// AbstractClassWithoutAbstractMethod: This class should not be instantiated, even if it is not by the pure definition an abstract class
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class MyCustomWorld extends Specification {
  def testRan = false

  @Shared
  def state = 0
}
