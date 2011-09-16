package pease.gherkin.model

import groovy.transform.TupleConstructor
import pease.gherkin.GherkinVisitor

@TupleConstructor(includeSuperProperties = true)
class ScenarioOutlineNode extends TaggedNode {
  final String name
  final List<TemplateStepNode> steps = []

  TableNode examples

  @Override
  void visit(GherkinVisitor visitor) {
    visitor.visitScenarioOutlineNode(this)
  }
}
