package pease.groovy.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import org.codehaus.groovy.ast.expr.Expression
import pease.gherkin.model.TableNode
import pease.support.SpockTestClass

@TupleConstructor
@EqualsAndHashCode
@SpockTestClass('ConcreteStepSpec')
class TableParam extends StepParam {
  TableNode table

  @Override
  Expression getExpression() {
    table.toListAst()
  }
}
