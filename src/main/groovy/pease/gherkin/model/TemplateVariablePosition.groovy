package pease.gherkin.model

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@TupleConstructor
@EqualsAndHashCode
class TemplateVariablePosition {
  int startPosition
  int endPosition

  /** name that was replaced */
  String variableName
}
