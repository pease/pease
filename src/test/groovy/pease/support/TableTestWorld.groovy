package pease.support

import spock.lang.Specification

// AbstractClassWithoutAbstractMethod: This class should not be instantiated, even if it is not by the pure definition an abstract class
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class TableTestWorld extends Specification {

  def filterTable(table) {
    [table.head().toList()] +
        table.tail().grep { user, email ->
          email =~ ~/\.de/
        }
  }

  // store the users record here to access it from outside of the test (another place may be a 'def' in Given)
  def users = null
}
