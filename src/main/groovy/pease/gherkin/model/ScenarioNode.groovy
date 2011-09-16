package pease.gherkin.model

import groovy.transform.TupleConstructor
import pease.gherkin.GherkinVisitor

@TupleConstructor(includeFields = true, includeSuperProperties=true)
class ScenarioNode extends TaggedNode {
  final String name
  final List<StepNode> steps = []

  boolean equals(ScenarioNode scenarioNode) {
    scenarioNode == null ? false : name == scenarioNode.name
  }

  @Override
  void visit(GherkinVisitor visitor) {
    visitor.visitScenarioNode(this)
  }
}
