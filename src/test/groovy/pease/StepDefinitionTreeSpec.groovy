package pease

import pease.gherkin.FeatureLoader
import pease.gherkin.model.StepNode
import pease.gherkin.model.TemplateStepNode
import pease.groovy.StepLoader
import spock.lang.Specification
import pease.groovy.model.*

class StepDefinitionTreeSpec extends Specification {
  def feature = FeatureLoader.instance.loadFromString('''
    Feature: foo
      Scenario: normal scenario
        When this scenario got matched
          | and         | a  table |
          | is attached | to it    |
        Then I can test its behaviour
        And we can test that
          """
          attached doc strings are also handled
          """

      Scenario Outline: bar
        When I do "<this>" and that
        Then I get <that>
        And we can test that
          """
          attached doc strings are also handled
          """
        Examples:
          | this            | that       |
          | add two numbers | their sum  |
          | calculate pi    | 3.14159265 |
  '''.stripIndent()).fromMaybe(null)

  def stepDefinitionTree = StepLoader.instance.loadFromString(/
    When(~\/(\w+) scenario got matched\/) { whatScenario, theTable -> }
    Then(~\/we can test that\/) { whatToTest -> }

    When(~\/I do "([^"]*)" (.*)\/) { whatIDid, andElse -> }
    Then(~\/I get (\d*\.\d*)\/) { whatIGet -> }
  /.stripIndent())

  def firstScenarioOutline = feature.scenarioOutlines[0]
  def firstScenario = feature.scenarios[0]

  def 'StepNode matches should include the matched code and all params'() {
    given:
    StepNode firstStep = firstScenario.steps.first()

    and:
    def expectedParams = [
        new ValueParam('this'),
        new TableParam(firstStep.table)
    ]

    when:
    def concreteStep = stepDefinitionTree.findConcreteStep(firstStep).fromMaybe(null)

    then:
    concreteStep?.closureExpression?.parameters*.name == ['whatScenario', 'theTable']

    and:
    concreteStep?.stepParams == expectedParams
  }

  def 'TemplateStepNode matches should include the matched templates and texts as params'() {
    given:
    TemplateStepNode firstTemplateStep = firstScenarioOutline.steps.first()

    when:
    ConcreteStep concreteStep = stepDefinitionTree.findConcreteStep(firstTemplateStep, firstScenarioOutline.examples).fromMaybe(null)

    then:
    concreteStep?.stepParams == [new VariableParam('this'), new ValueParam('and that')]

    and:
    concreteStep?.closureExpression?.parameters*.name == ['whatIDid', 'andElse']

  }

  def 'No ConcreteStep should be found when no common step definition for a TemplateStepNode can be found'() {
    when:
    def maybeConcreteStep = stepDefinitionTree.findConcreteStep(
        firstScenarioOutline.steps[1],
        firstScenarioOutline.examples
    )

    then:
    maybeConcreteStep.isNothing()
  }

  def 'No ConcreteStep should be returned, when no step definition for a StepNode can be found'() {
    expect:
    stepDefinitionTree.findConcreteStep(firstScenario.steps[1]).isNothing()
  }

  def 'multiple StepDefinitionTrees should be merged into a single tree'() {
    given:
    def tree1 = new StepDefinitionTree()
    tree1.steps << new StepDefinitionNode() << new StepDefinitionNode()
    tree1.worlds << new WorldNode()
    tree1.hooks << new HookNode()

    and:
    def tree2 = new StepDefinitionTree()
    tree2.steps << new StepDefinitionNode()
    tree2.hooks << new HookNode()
    tree2.worlds << new WorldNode()

    when:
    tree1.merge(tree2)

    then:
    tree1.steps.size() == 3

    and:
    tree1.hooks.size() == 2

    and:
    tree1.worlds.size() == 2
  }

  def 'doc string step arguments should be returned as closure parameters'() {
    given:
    def params = [new ValueParam('attached doc strings are also handled')]

    when:
    def concreteStep = stepDefinitionTree.findConcreteStep(firstScenario.steps[2]).fromMaybe(null)

    and:
    def concreteTemplateStep = stepDefinitionTree.findConcreteStep(firstScenarioOutline.steps[2], firstScenarioOutline.examples).fromMaybe(null)

    then:
    concreteStep?.stepParams == params
    concreteTemplateStep?.stepParams == params
  }
}