package pease.groovy

import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import pease.groovy.model.ConcreteStep
import pease.groovy.model.StepDefinitionNode
import pease.groovy.model.ValueParam
import pease.groovy.model.VariableParam
import spock.lang.Specification
import static pease.StepKind.WHEN

class ConcreteStepSpec extends Specification {
  def "the statements of a closure should be rewritten and the variables replaced"() {
    given:
    def nodes = new AstBuilder().buildFromString """
      { a, b ->
        foo = a        // BinaryExpression(VariableExpression, '=', VariableExpression)
        aMethod(b)     // MethodCallExpression
      }
    """.stripIndent()
    def closure = nodes[0].statements[0].expression

    and:
    def params = [new ValueParam(5), new VariableParam("fooBar")]
    def stepCode = new ConcreteStep(new StepDefinitionNode(WHEN, ~/foo/, closure), params)

    when:
    def statements = stepCode.rewriteClosure()

    then: 'value parameters should be replaced'
    statements[0].expression.rightExpression.class == ConstantExpression
    statements[0].expression.rightExpression.value == 5

    and: 'the variable name should be normalized'
    statements[1].expression.arguments.expressions[0].class == VariableExpression
    statements[1].expression.arguments.expressions[0].variable == 'fooBar'
  }

}
