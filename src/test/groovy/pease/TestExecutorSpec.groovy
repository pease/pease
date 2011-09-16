package pease

import pease.support.CapturePrintStream
import spock.lang.Specification

class TestExecutorSpec extends Specification {
  final static String RESOURCES_DIRECTORY = 'src/test/resources'

  def executor = new TestExecutor(RESOURCES_DIRECTORY, "$RESOURCES_DIRECTORY/step_definitions")

  def featureFiles = [
        'Simple',
        'Table',
        'Tagged',
        'TopLevelScenario',
        'user_management',
        'web_search'
  ]

  def 'feature files are found in the default directory'() {
    when:
    def foundFeatureFiles = FileLoader.instance.findFeatures(RESOURCES_DIRECTORY)

    then:
    foundFeatureFiles as Set == featureFiles.collect {"$RESOURCES_DIRECTORY/${it}.feature"} as Set
  }

  def 'load features'() {
    when:
    def features = executor.loadFeatures()

    then:
    // one feature will not be loaded, because it's invalid
    features.size() == featureFiles.size() - 1
  }

  def 'step definitions are found in the step_definitions directory'() {
    when:
    def steps = FileLoader.instance.findSteps("$RESOURCES_DIRECTORY/step_definitions") as Set

    then:
    steps == ['filterMath.groovy', 'simple.groovy'].collect { "$RESOURCES_DIRECTORY/step_definitions/$it" }  as Set
  }

  def 'load all step definitions'() {
    when:
    def loadedSteps = executor.loadSteps()

    then:
    loadedSteps.steps.size() == 6

    and:
    loadedSteps.steps[0].regex.pattern() == 'I have following users'

    and:
    loadedSteps.steps.find {it.kind == StepKind.THEN }.regex.pattern() == 'I should have the following users'
  }

  def 'execute features'() {
    given:
    def stream = new CapturePrintStream()

    and:
    System.setOut(stream)

    and:
    def expectedResult = '''\
      Feature: User management

        Scenario: Filter users
          Given I have following users           # src/test/resources/step_definitions/filterMath.groovy:5
          When I filter those from Germany       # src/test/resources/step_definitions/filterMath.groovy:9
          Then I should have the following users # src/test/resources/step_definitions/filterMath.groovy:13
    '''.stripIndent()


    when:
    executor.runFeatures(new Configuration(tagExpression: ['@run'], useColor: false))

    then:
    stream.toString() == expectedResult
  }
}