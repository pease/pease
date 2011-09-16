package pease.groovy.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import pease.support.SpockTestClass

@TupleConstructor
@EqualsAndHashCode
@SpockTestClass('ConcreteStepSpec')
/** A instance of this class should always be an valid java identifier! */
class VariableParam extends StepParam {
  String variableName

  @Override
  Expression getExpression() {
    new VariableExpression(variableName)
  }
}
