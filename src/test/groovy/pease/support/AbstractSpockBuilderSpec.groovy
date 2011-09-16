package pease.support

import org.junit.runner.Result
import org.junit.runner.notification.RunNotifier
import pease.gherkin.FeatureLoader
import pease.groovy.StepLoader
import pease.groovy.model.StepDefinitionTree
import pease.spock.SpockBuilder
import spock.lang.Specification

// AbstractClassWithoutAbstractMethod: This class should not be instantiated, even if it is not by the pure definition an abstract class
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class AbstractSpockBuilderSpec extends Specification {
  def featureLoader = FeatureLoader.instance
  def stepLoader = StepLoader.instance
  def spockBuilder = SpockBuilder.instance

  def parseFeature(feature) {
    featureLoader.loadFromString(feature).fromMaybe(null)
  }

  def parseStepsInSingleScenario(steps, scenarioTags='') {
    def featureFile = """
      Feature: feature
        $scenarioTags
        Scenario: scenario
          $steps
    """

    parseFeature(featureFile)
  }

  def parseStepsInSingleScenarioOutline(steps, scenarioOutlineTags = '') {
    def feature = """
      Feature: feature
        $scenarioOutlineTags
        Scenario Outline: scenario
          $steps
        Examples:
          | a | b |
          | 1 | 2 |
    """

    parseFeature(feature)
  }

  StepDefinitionTree parseSteps(steps) {
    stepLoader.loadFromString(steps)
  }

  def runSpecWithSpySputnikRunner(Class clazz) {
    def result = new Result()
    def listener = result.createListener()
    def notifier = new RunNotifier()
    notifier.addListener(listener)

    def sputnik = new SpySputnik(clazz)
    notifier.fireTestStarted(sputnik.description)
    sputnik.run(notifier)
    notifier.fireTestRunFinished(result)

    [sputnik, result]
  }

  def testWasExecuted(sputnik, indicatorVariable) {
    sputnik.currentInstance?."$indicatorVariable"
  }

  def mkEmptySteps() {
    new StepDefinitionTree()
  }
}
