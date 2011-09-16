package pease

import groovy.transform.TupleConstructor
import org.junit.runner.JUnitCore
import pease.gherkin.FeatureLoader
import pease.gherkin.model.FeatureNode
import pease.groovy.StepLoader
import pease.groovy.model.StepDefinitionTree
import pease.support.Maybe.Nothing

@TupleConstructor
@SuppressWarnings(['Println'])
class TestExecutor {
  String featureDirectory = '.'
  String stepsDirectory = './step_definitions'

  final junit = new JUnitCore()
  final stepLoader = StepLoader.instance
  final featureLoader = FeatureLoader.instance


  StepDefinitionTree loadSteps() {
    stepLoader.loadSteps(stepsDirectory)
  }

  List<FeatureNode> loadFeatures() {
    featureLoader.loadFeatures(featureDirectory)
  }

  void runFeatures(Configuration configuration) {
    def features = loadFeatures()
    def steps = loadSteps()

    for (feature in features) {
      feature.compile(steps, configuration) >>> { clazz -> executeTest(clazz); Nothing.instance }
    }
  }

  void executeTest(Class spockTestClass) {
    def result = junit.runClasses(spockTestClass)

    if (result.failureCount > 0) {
      result.failures.each {
        println "\nTest failure: $it.description"
        println it.trace
      }
    }
  }

  static void main(String[] args) {
    def tags = []
    def executor = new TestExecutor('./features', './features/step_definitions')

    def cli = new CliBuilder(expandArgumentFiles: false)
    cli.with {
      h longOpt: 'help', 'Show usage information'
      t args: 1, longOpt: 'tags', 'Run only features matching this tag expression'
    }
    def options = cli.parse(args)

    if (options.tags) {
      tags << options.tags
    }

    def configuration = new Configuration(tagExpression: tags, useColor: true)

    executor.runFeatures(configuration)
  }
}
