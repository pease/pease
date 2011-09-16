package pease.gherkin

import gherkin.formatter.Formatter
import pease.StepKind
import pease.support.SpockTestClass
import gherkin.formatter.model.*
import pease.gherkin.model.*

// StatelessClass: CodeNarc marks this class as @Stateless by accident
@SuppressWarnings(['StatelessClass'])
@SpockTestClass('PeaseFormatterSpec')
class PeaseFormatter implements Formatter {
  private FeatureNode lastFeature
  private ScenarioNode lastScenario
  private ScenarioOutlineNode lastScenarioOutline
  private StepNode lastStep

  // flag to save if the last read `Scenario' was a `Scenario' or a `Scenario Outline'
  private readScenario = false

  FeatureNode getFeature() {
    lastFeature
  }

  Tags makeTags(List<Tag> tags) {
    def result = new Tags()
    result.addAll(tags?.name ?: [])
    result
  }

  def buildTableFrom(def tableList) {
    def rows = []

    tableList.each { row ->
      rows << new RowNode(row.cells)
    }

    new TableNode(rows)
  }

  void setLastScenario(ScenarioNode scenarioNode) {
    readScenario = true
    lastScenario = scenarioNode
  }

  void setLastScenarioOutline(ScenarioOutlineNode scenarioOutlineNode) {
    readScenario = false
    lastScenarioOutline = scenarioOutlineNode
  }

  @Override
  void uri(String uri) {
  }

  @Override
  void feature(gherkin.formatter.model.Feature feature) {
    lastFeature = new FeatureNode(makeTags(feature.tags), feature.name)
  }

  @Override
  void background(Background background) {
  }

  @Override
  void scenario(Scenario scenario) {
    if (lastFeature != null) {
      setLastScenario(new ScenarioNode(lastFeature.tags + makeTags(scenario.tags), scenario.name, []))

      lastFeature.scenarios << lastScenario
    }
  }

  @Override
  void scenarioOutline(ScenarioOutline scenarioOutline) {
    if (lastFeature != null) {
      setLastScenarioOutline(new ScenarioOutlineNode(makeTags(scenarioOutline.tags), scenarioOutline.name, []))
      lastFeature.scenarioOutlines << lastScenarioOutline
    }
  }

  @Override
  void examples(Examples examples) {
    if (lastScenarioOutline != null) {
      lastScenarioOutline.examples = buildTableFrom(examples.rows)
    }
  }

  @Override
  void step(Step step) {
    def kind = (step.keyword =~ /^And/ && lastStep) ? lastStep.kind : StepKind.fromString(step.keyword)
    def table = null
    def docString = null

    if (step.multilineArg) {
      switch (step.multilineArg?.class) {
        case DocString.class:
          docString = ((DocString) step.multilineArg).value
          break

        case ArrayList.class:
          table = buildTableFrom(step.multilineArg)
          break

        case null:
          break

      }
    }

    lastStep = new StepNode(kind, step.name, table, docString)

    if (readScenario && lastScenario != null) {
      lastScenario.steps.add(lastStep)
    } else if (!readScenario && lastScenarioOutline != null) {
      lastScenarioOutline.steps.add(new TemplateStepNode(lastStep))
    } else {
      // forget about that step, because we have no lastScenario or lastScenarioOutline
      lastStep = null
    }
  }

  @Override
  void eof() {
  }

  @Override
  void syntaxError(String state, String event, List<String> legalEvents, String uri, int line) {
  }
}
