package pease.gherkin

import pease.gherkin.model.FeatureNode
import pease.gherkin.model.ScenarioOutlineNode
import pease.gherkin.model.ScenarioNode
import pease.gherkin.model.StepNode
import pease.gherkin.model.TableNode
import pease.gherkin.model.TemplateStepNode

class GherkinVisitorSupport implements GherkinVisitor {
  @Override
  void visitFeatureNode(FeatureNode featureNode) {
    featureNode.scenarios.each { it.visit(this) }
    featureNode.scenarioOutlines.each { it.visit(this) }
  }

  @Override
  void visitScenarioOutlineNode(ScenarioOutlineNode scenarioOutlineNode) {
    scenarioOutlineNode.steps.each { it.visit(this) }
    scenarioOutlineNode.examples.visit(this)
  }

  @Override
  void visitScenarioNode(ScenarioNode scenarioNode) {
    scenarioNode.steps.each { it.visit(this) }
  }

  @Override
  void visitStepNode(StepNode stepNode) {
  }

  @Override
  void visitExamples(TableNode tableNode) {
  }

  @Override
  void visitTemplateStepNode(TemplateStepNode templateStepNode) {
  }
}
