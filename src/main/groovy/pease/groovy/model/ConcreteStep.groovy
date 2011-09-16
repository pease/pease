package pease.groovy.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import pease.groovy.ParameterReplacementVisitor
import pease.support.SpockTestClass

// A matched StepDefinitionNode for a StepNode with the matched step parameter
@TupleConstructor
@EqualsAndHashCode(excludes = 'stepParams')
@SpockTestClass('ConcreteStepSpec')
class ConcreteStep {
  @Delegate StepDefinitionNode matchedCode
  List<StepParam> stepParams

  List<Statement> rewriteClosure() {
    if (closureExpression?.parameters?.size() != stepParams.size()) {
      throw new IllegalArgumentException('wrong number of parameters')
    }

    if (closureExpression.code instanceof BlockStatement) {
      def visitor = new ParameterReplacementVisitor()

      stepParams.eachWithIndex { param, index ->
        def variableName = closureExpression.parameters[index].name
        visitor.replacements[variableName] = param.expression
      }

      closureExpression.code.visit(visitor)

      ((BlockStatement) closureExpression.code).statements
    } else {
      // TODO error handling, when there is no BlockStatement (when is that the case?)
      []
    }
  }
}
