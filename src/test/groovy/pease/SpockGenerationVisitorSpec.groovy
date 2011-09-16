package pease

import java.util.regex.Pattern
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.spockframework.compiler.AstUtil
import pease.gherkin.FeatureLoader
import pease.gherkin.SpockGenerationVisitor
import pease.gherkin.model.ScenarioOutlineNode
import pease.gherkin.model.StepNode
import pease.gherkin.model.Tags
import pease.gherkin.model.TemplateStepNode
import pease.groovy.StepLoader
import spock.lang.Specification
import static pease.StepKind.*
import pease.groovy.model.*

class SpockGenerationVisitorSpec extends Specification {
  def visitor = new SpockGenerationVisitor()

  def feature = FeatureLoader.instance.loadFromString('''
    Feature: foo
      Scenario Outline: bar
        When I add <a number> and <another (interesting) number>
        Then I want <c>
        Examples:
          | a number  | another (interesting) number  | c  |
          | 1         | 2                             | 3  |
          | 42        | 23                            | 65 |
  ''').fromMaybe(null)

  def steps = StepLoader.instance.loadFromString '''
    When(~/I add (\\d+) and (\\d+)/) { myA, myB ->
       def result = a + b
    }
    Then(~/I want (\\d+)/) { myC ->
       result == c
    }
  '''.stripIndent()

  //--------------------------------------------------------------------------------
  // End test wide instances
  //--------------------------------------------------------------------------------

  ClosureExpression mkEmptyClosure() {
    new ClosureExpression(Parameter.EMPTY_ARRAY, null)
  }

  def mkConcreteStep(StepDefinitionNode stepCodeNode) {
    new ConcreteStep(stepCodeNode, [])
  }

  //--------------------------------------------------------------------------------
  // End helper methods
  //--------------------------------------------------------------------------------

  def 'created blocks should contain a labeled expression with the step name '() {
    given:
    def stepNode = new StepNode(GIVEN, 'my text', null)

    and:
    def stepCodeNode = new StepDefinitionNode(GIVEN, ~/my text/, mkEmptyClosure())
    def concreteStep = mkConcreteStep(stepCodeNode)

    and:
    def lastKind = null

    when:
    def block = visitor.createBlock(lastKind, stepNode, concreteStep)

    then: 'there are at least two statements'
    block.statements.size() >= 2

    and: 'the first statement should be the name of the step'
    def firstStatement = block.statements[0]
    firstStatement.statementLabel == 'given'
    AstUtil.getExpression(firstStatement, ConstantExpression).text == 'my text'

    and: 'the second statement should be the filename of the associated step definition'
    AstUtil.getExpression(block.statements[1], ConstantExpression).text.startsWith('file:')
  }

  def 'created methods should contain all assigned blocks'() {
    given:
    List<SpockBlock> blocks = [
        [WHEN, 'this'],
        [AND, 'that'],
        [THEN, 'foo'],
        [AND, 'bar']
    ].collect { StepKind kind, String text ->
      visitor.createBlock(
          null,
          new StepNode(kind, text, null),
          mkConcreteStep(new StepDefinitionNode(kind, Pattern.compile(text), mkEmptyClosure()))
      )
    }

    when:
    def methodNode = visitor.createMethodNode("foobar", blocks)

    then:
    methodNode.name == "foobar"

    and:
    methodNode.code.statements.findAll { it?.statementLabel != null }*.statementLabel == ["when", "and", "then", "and"]
  }

  def 'pending blocks should be build when no matching step definition can be assigned'() {
    given:
    def stepNode = new StepNode(GIVEN, 'not yet implemented', null)

    and:
    visitor.stepDefinitionTree = new StepDefinitionTree()

    when:
    visitor.visitStepNode(stepNode)

    then:
    visitor.blocks.any { it instanceof PendingSpockBlock }
  }

  def 'pending blocks for unmatched steps inside a scenario outline node should be created'() {
    given:
    def scenarioOutlineNode = new ScenarioOutlineNode(new Tags(), 'scenario outline', [])
    scenarioOutlineNode.steps << new TemplateStepNode(new StepNode(StepKind.GIVEN, 'given', null, null))

    and: 'there is an empty step definition tree'
    visitor.stepDefinitionTree = new StepDefinitionTree()

    when:
    visitor.visitScenarioOutlineNode(scenarioOutlineNode)

    then:
    def method = visitor.methodNodes.find { it.name == 'scenario outline' }
    method.code.statements.any { it instanceof ExpressionStatement && it.expression instanceof ConstantExpression && it.expression.text == 'file:pending' }
  }

  def 'spock feature methods that have pending blocks should have the @Ignore annotation'() {
    given:
    def blocks = [visitor.createPendingBlock(WHEN, new StepNode(WHEN, 'I do this'))]

    when:
    def methodNode = visitor.createMethodNode('method foo', blocks)

    then:
    methodNode.annotations.any { AnnotationNode node -> node.classNode.name.endsWith('Ignore') }
  }

  def 'where block should be generated when building scenario outlines'() {
    given:
    visitor.stepDefinitionTree = steps

    when:
    visitor.visitScenarioOutlineNode(feature.scenarioOutlines[0])

    then: 'a where block has been generated'
    SpockBlock whereBlock = visitor.blocks.find { block -> block.statements.any {Statement stmt -> stmt.statementLabel == 'where' } }
    whereBlock != null

    and: 'the where block contains enough rows'
    List<ExpressionStatement> exprs = whereBlock.statements[1..-1].findAll { it.class == ExpressionStatement && it.expression.class == BinaryExpression }
    exprs.size() == 3

    and: 'the names in the first row are valid identifiers'
    whereBlock.statements[1].expression.class == BinaryExpression
    whereBlock.statements[1].expression.leftExpression.class == BinaryExpression
    whereBlock.statements[1].expression.leftExpression.leftExpression.variable == 'aNumber'
    whereBlock.statements[1].expression.leftExpression.rightExpression.variable == 'anotherInterestingNumber'
  }
}