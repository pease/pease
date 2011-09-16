package pease.groovy.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import pease.support.SpockTestClass

@TupleConstructor
@EqualsAndHashCode
@SpockTestClass('ConcreteStepSpec')
class ValueParam extends StepParam {
  Object value

  @Override
  Expression getExpression() {
    new ConstantExpression(value)
  }
}
