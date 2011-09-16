package pease.groovy.model

import org.codehaus.groovy.ast.expr.Expression
import pease.support.SpockTestClass

@SpockTestClass('ConcreteStepSpec')
abstract class StepParam {
  abstract Expression getExpression()
}
