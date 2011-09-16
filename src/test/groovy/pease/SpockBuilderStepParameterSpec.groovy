package pease

import java.util.regex.Pattern
import org.codehaus.groovy.ast.builder.AstBuilder
import pease.gherkin.model.StepNode
import pease.groovy.model.StepDefinitionNode
import pease.support.AbstractSpockBuilderSpec

class SpockBuilderStepParameterSpec extends AbstractSpockBuilderSpec {
  def astBuilder = new AstBuilder()

  def "matching group"(Pattern pattern, String input, List expect) {
    given:
    def stepCodeNode = new StepDefinitionNode(StepKind.GIVEN, pattern, null)

    and:
    def maybeList = stepCodeNode.matchStepNode(new StepNode(StepKind.GIVEN, input, null))

    expect:
    maybeList.isJust()

    and:
    maybeList.fromJust() == expect

    where:
    pattern              | input            | expect
    ~/hello/             | "hello"          | []
    ~/t(.)(..)pattern/   | "testpattern"    | [["e", 1, 2], ["st", 2, 4]]
    ~/number (\d+) here/ | "number 42 here" | [["42", 7, 9]]
  }

  def "rewrite assignment statement throws exception"() {
    given:
    def nodes = astBuilder.buildFromString """
      { foo ->
        def localVariable = 2
        $assignment
      }
    """
    def closure = nodes[0].statements[0].expression

    and:
    def params = [42]

    when:
    spockBuilder.rewriteClosureToStatementList(closure, params)

    then:
    thrown RuntimeException

    where:
    assignment << [
        "foo = localVariable + 21",
        "++foo",
        "foo++",
        "(localVariable, foo) = [0, 1]"
    ]
  }

  def "simple step feature"() {
    given: "a gherkin feature"
    def feature = parseFeature """
      Feature: Steps
        Scenario: test steps
          When I type "5" into my calculator
          Then I should see "5" on the display
    """

    and: "step definitions"
    def steps = parseSteps """
      World(pease.support.MyCustomWorld)

      When ~/I type "(\\d+)" into my (\\w+)/, { num, thing ->
        state = num
        assert thing == "calculator"
      }

      Then ~/I should see "(\\d+)" on the (\\w+)/, { num, output ->
        state == num
        output == "display"
      }
    """

    and: "a compiled test"
    def spec = spockBuilder.compile(feature, steps).fromJust()

    when: "running the test"
    def (sputnik, result) = runSpecWithSpySputnikRunner(spec)

    then: "there should be no errors"
    result.failures == []

    and: "the test should be executed"
    testWasExecuted(sputnik, "state")
  }

  def "table parameter"() {
    given:
    def feature = parseStepsInSingleScenario """
      Given I have following users
        | user    | email                |
        | gcantor | george@cantor.de     |
        | leibniz | gottfried@leibniz.de |
        | hcurry  | haskell@curry.com    |

      When I filter those from Germany

      Then I should have the following users
        | user    | email                |
        | gcantor | george@cantor.de     |
        | leibniz | gottfried@leibniz.de |
    """
    def steps = parseSteps """
      World(pease.support.TableTestWorld)

      Given ~/I have following users/, { table ->
        users = table
      }
      When ~/I filter those from Germany/, {
        users = filterTable users
      }
      Then ~/I should have the following users/, { table ->
        users == table
      }
    """
    when:
    def spec = spockBuilder.compile(feature, steps).fromJust()

    and:
    def (sputnik, result) = runSpecWithSpySputnikRunner(spec)

    then:
    result.failures == []

    and:
    testWasExecuted(sputnik, "users")
  }

  def "wrong number of parameters"() {
    given: "gherkin feature"
    def feature = parseFeature """
      Feature: feature
        Scenario: scenario
          Given there "are" "two" parameter
    """

    and:
    def steps = parseSteps stepDefinition

    when:
    spockBuilder.buildAST feature[0], steps

    then:
    thrown RuntimeException

    where:
    purpose                      | stepDefinition
    "too few closure parameter"  | 'Given ~/there "(.+) "(.+)" parameter/, { onlyOneVariable -> }'
    "too much closure parameter" | 'Given ~/there "(.+) "(.+)" parameter/, { a, b, c -> }'
  }
}