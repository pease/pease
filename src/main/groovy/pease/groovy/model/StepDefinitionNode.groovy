package pease.groovy.model

import groovy.transform.TupleConstructor
import java.util.regex.Pattern
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.spockframework.util.Nullable
import pease.StepKind
import pease.gherkin.model.StepNode
import pease.support.Maybe
import pease.support.Maybe.Just
import pease.support.Maybe.Nothing

// Represents a code block that executes a test step
@TupleConstructor
class StepDefinitionNode {
  final StepKind kind
  final Pattern regex
  final ClosureExpression closureExpression
  @Nullable String filename

  Maybe<List> matchStepNode(StepNode stepNode) {
    def matchedGroups = []

    def matcher = stepNode.text =~ regex

    if (matcher.find()) {
      // Too bad Groovy doesn't recognize 1.upto(0) as empty loop ...
      if (matcher.groupCount() > 0) {
        1.upto(matcher.groupCount()) {
          matchedGroups << [matcher.group(it), matcher.start(it), matcher.end(it)]
        }
      }

      new Just(matchedGroups)
    } else {
      Nothing.instance
    }
  }
}
