package pease

import java.lang.reflect.Method
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.spockframework.runtime.model.FeatureMetadata
import org.spockframework.runtime.model.SpecMetadata
import pease.gherkin.model.FeatureNode
import pease.groovy.model.StepDefinitionTree
import pease.support.AbstractSpockBuilderSpec
import pease.support.CapturePrintStream

class SpockBuilderSpec extends AbstractSpockBuilderSpec {
  def feature = """
    Feature: Foobar feature that costs 10.000 €
        Scenario: My first scenario
            Given I have a feature file
            When I load the feature file
            Then the step definitions get loaded, too
    """.stripIndent()

  def steps = """\
        Given(~/I have a feature file/) {                        // line 1
        }

        When(~/I load the feature file/) {                       // line 4
        }

        Then(~/the step definitions get loaded, too/) {          // line 7
        }
    """.stripIndent()

  FeatureNode featureNode = featureLoader.loadFromString(feature).fromMaybe(null)
  StepDefinitionTree stepDefinitions = parseSteps(steps)

  def "a spock class should be build from a feature and step definitions"() {
    when: 'the spock builder is invoked'
    def classNode = spockBuilder.buildAST(featureNode, stepDefinitions).fromMaybe(null)

    then: 'the resulting class should be annotated with the full name'
    ConstantExpression specNameValue = (ConstantExpression) classNode.annotations.find {
      it.classNode.name == "spock.lang.SpecName"
    }?.members['value']

    specNameValue?.value == 'Foobar feature that costs 10.000 €'

    and: "each scenario is represented as method"
    featureNode.scenarios.each { scenario ->
      assert classNode.methods.find { scenario.name == it.name }
    }
  }

  def "a compiled junit test class should be the result"() {
    when: 'the test is compiled'
    def clazz = spockBuilder.compile(featureNode, stepDefinitions).fromMaybe(null)

    then: 'the feature-name is annotation in the SpecMetadata'
    clazz.getAnnotation(SpecMetadata)?.fullname() == "Foobar feature that costs 10.000 €"

    and:
    List<Method> features = clazz.methods.findAll { it.getAnnotation(FeatureMetadata) }
    features.size() == 1

    and:
    features.head().getAnnotation(FeatureMetadata).blocks()*.fileOrigins() == [[':1'],[':4'],[':7']]
  }

  def "execute parametrized step definitions"() {
    given:
    def featureWithParameter = featureLoader.loadFromString '''
        Feature: Test execution of parameterized steps

          Scenario: Scenario with parameterized steps
            When I put 5 into the calculator
            When I press the double key
            Then the result should be 10
    ''' fromMaybe(null)

    def expectedResult = '''\
        Feature: Test execution of parameterized steps

          Scenario: Scenario with parameterized steps
            When I put 5 into the calculator # :5
            When I press the double key      # :8
            Then the result should be 10     # :13
    '''.stripIndent()

    and: 'a step definition for the feature definition'
    def stepsForFeatureWithParameter = stepLoader.loadFromString '''\
        import pease.support.MyCustomWorld

        World MyCustomWorld.class

        When ~/I put (\\d+) into the calculator/, { num ->   // line 5
          def input = 5
        }
        When ~/I press the double key/, {                    // line 8
          input = input * 2
          // trick to check if the code ran
          testRan = true
        }
        Then ~/the result should be 10/, {                   // line 13
          input == 10
        }
    '''

    and: 'a printstream that captures stdout'
    def stream = new CapturePrintStream()
    System.setOut(stream)

    and: 'a compiled spock spec'
    featureWithParameter.fileName = 'testExecutionOfParameterizedSteps.feature'
    Class spec = spockBuilder.compile(featureWithParameter, stepsForFeatureWithParameter).fromJust()

    when: 'the test gets executed'
    def (sputnik, result) = runSpecWithSpySputnikRunner(spec)

    then: 'the Spock test was executed'
    sputnik.currentInstance?.testRan

    and: 'the output wrote by the test equals the specified gherkin file'
    stream.toString() == expectedResult

    and: 'the test execution was succesful'
    result.wasSuccessful()
  }
}
