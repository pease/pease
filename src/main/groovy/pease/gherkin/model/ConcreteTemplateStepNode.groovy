package pease.gherkin.model

import groovy.transform.TupleConstructor

@TupleConstructor(includeSuperProperties=true)
class ConcreteTemplateStepNode extends StepNode {
  List<TemplateVariablePosition> variablePositions
}
