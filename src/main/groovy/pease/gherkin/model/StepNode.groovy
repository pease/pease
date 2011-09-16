package pease.gherkin.model

import groovy.transform.TupleConstructor
import pease.StepKind
import pease.gherkin.GherkinVisitor

@TupleConstructor
class StepNode extends GherkinNode {
  StepKind kind
  String text
  TableNode table
  String docString

  @Override
  void visit(GherkinVisitor visitor) {
    visitor.visitStepNode(this)
  }
}
