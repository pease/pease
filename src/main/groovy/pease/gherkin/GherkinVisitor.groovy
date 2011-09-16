package pease.gherkin

import pease.gherkin.model.FeatureNode
import pease.gherkin.model.ScenarioNode
import pease.gherkin.model.ScenarioOutlineNode
import pease.gherkin.model.TableNode
import pease.gherkin.model.StepNode
import pease.gherkin.model.TemplateStepNode

interface GherkinVisitor {
  void visitFeatureNode(FeatureNode featureNode)
  void visitScenarioNode(ScenarioNode scenarioNode)
  void visitScenarioOutlineNode(ScenarioOutlineNode scenarioOutlineNode)

  void visitExamples(TableNode tableNode)

  void visitStepNode(StepNode stepNode)
  void visitTemplateStepNode(TemplateStepNode templateStepNode)
}