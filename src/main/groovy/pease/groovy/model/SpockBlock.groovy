package pease.groovy.model

import groovy.transform.TupleConstructor
import org.codehaus.groovy.ast.stmt.Statement

@TupleConstructor
class SpockBlock {
  List<Statement> statements
}
