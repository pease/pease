package pease

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import pease.gherkin.SpockGenerationVisitor
import pease.support.AbstractSpockBuilderSpec

class SpockBuilderHooks extends AbstractSpockBuilderSpec {
  def 'each test method of a should contain a global hook'(String hook, int statementIndex, boolean scenarioOutline) {
    given: 'there is an global "before" hook defined'
    def steps = """
      $hook {
        def foo = 'bar';
      }
      Given(~/given/) {}
      When(~/when/) {}
      Then(~/then/) {}
    """
    def stepDefinitions = parseSteps(steps)

    and:
    def featureNode = (scenarioOutline ? this.&parseStepsInSingleScenarioOutline : this.&parseStepsInSingleScenario) '''
      Given given
      When when
      Then then
    '''

    when:
    ClassNode ast = spockBuilder.buildAST(featureNode, stepDefinitions).fromMaybe(null)

    then: 'each method must contain the above defined assignment statement'
    def methodCode = ast.methods.find { it.name == 'scenario' }.code
    methodCode instanceof BlockStatement && methodCode.statements[statementIndex].expression instanceof DeclarationExpression

    where:
    hook     | statementIndex | scenarioOutline
    'Before' | 0              | false
    'After'  | -1             | false
    'Before' | 0              | true
    'After'  | -1             | true
  }

  def 'multiple global hooks should be accumulated'() {
    def stepDefinitions = parseSteps """
      $hookName {
        def a = []
      }
      $hookName {
        def b = []
      }
    """

    def feature = this.&parseStepsInSingleScenario '''
      Given given
      When when
      Then then
    '''

    when:
    ClassNode ast = spockBuilder.buildAST(feature, stepDefinitions).fromMaybe(null)

    then:
    MethodNode method = ast.methods.find { it.name == 'scenario'}
    method.code.statements[range].each {
      assert it.expression instanceof DeclarationExpression
    }

    where:
    hookName | range
    'Before' | 0..1
    'After'  | -2..-1
  }

  def 'tagged hooks should only be applied if the scenarios tags match'(String hook, boolean scenarioOutline) {
    def stepDefinitions = parseSteps """
      $hook('@a') {
        def a = []
      }

      $hook('@b') {
        def b = []
      }
    """

    def feature = (scenarioOutline ? this.&parseStepsInSingleScenarioOutline : this.&parseStepsInSingleScenario) '''
      Given given
      When when
      Then then
    ''', '@a'

    when:
    def visitor = new SpockGenerationVisitor(stepDefinitions, [])
    if (scenarioOutline) {
      visitor.visitScenarioOutlineNode(feature.scenarioOutlines.first())
    } else {
      visitor.visitScenarioNode(feature.scenarios.first())
    }

    then:
    // scenario outlines have an extra block due to the where-table
    visitor.blocks.size() == 4 + (scenarioOutline ?  1 : 0)

    where:
    hook     | scenarioOutline
    'Before' | false
    'After'  | false
    'Before' | true
    'After'  | true
  }
}
