package pease.groovy.model

import groovy.transform.TupleConstructor
import org.codehaus.groovy.ast.expr.ClosureExpression
import pease.gherkin.model.Tags

// unable to make this class @Immutable, because ClosureExpression is not @Immutable
@TupleConstructor
class HookNode {
  final HookKind kind
  final ClosureExpression closureExpression
  final List<Tags> tags = []
  String filename

  boolean matches(Tags query) {
    tags.any { Tags tag -> tag.matches(query) }
  }
}
