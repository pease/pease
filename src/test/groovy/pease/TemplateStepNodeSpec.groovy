package pease

import pease.gherkin.model.StepNode
import pease.gherkin.model.TemplateStepNode
import pease.gherkin.model.TemplateVariablePosition
import spock.lang.Specification

class TemplateStepNodeSpec extends Specification {
  def 'template variable replacement should work as expected'(String text, Map table, List varOrder, String newString) {
    given:
    def node = new TemplateStepNode(new StepNode(StepKind.GIVEN, text, null))

    when:
    def maybeConcreteTemplate = node.makeConcreteTemplateStepNode(table)

    then:
    maybeConcreteTemplate.isJust()

    and:
    maybeConcreteTemplate.fromJust().text == newString

    and:
    maybeConcreteTemplate.fromJust().variablePositions == varOrder.collect { new TemplateVariablePosition(* it) }

    where:
    text             | table            | varOrder                                  | newString
    '<a>b<c><d>'     | [a: 'A', d: 'D'] | [[0, 1, 'a'], [5, 6, 'd']]                | 'Ab<c>D'
    '<a> <a> <b><a>' | [a: 'blah']      | [[0, 4, 'a'], [5, 9, 'a'], [13, 17, 'a']] | 'blah blah <b>blah'
  }

  def 'TemplateStepNode template variables should be replaced with table data'(Map table, String template, String replaced) {
    given:
    def stepNode = new TemplateStepNode(new StepNode(StepKind.WHEN, template, null, null))

    when:
    def maybeConcreteTemplateStepNode = stepNode.makeConcreteTemplateStepNode(table)

    then:
    maybeConcreteTemplateStepNode.isJust()

    and:
    maybeConcreteTemplateStepNode.fromJust().text == replaced

    where:
    table                          | template                    | replaced
    ['abc': 'foo', 'de fg': 'bar'] | 'I have <abc> and <de fg>.' | 'I have foo and bar.'
    ['thing': 'template engine']   | 'I have a <thing>'          | 'I have a template engine'
  }

  def 'TemplateStepNode replacement should leave the step string unchanged when no parameters are present'(Map table, String template) {
    given:
    def templateStepNode = new TemplateStepNode(new StepNode(StepKind.WHEN, template, null, null))

    when:
    def maybeConcreteTemplateStepNode = templateStepNode.makeConcreteTemplateStepNode(table)

    then:
    maybeConcreteTemplateStepNode.isJust()

    and:
    maybeConcreteTemplateStepNode.fromJust().text == template

    where:
    table | template
    [:]   | '<only> a <header> and no replacement rows'
  }
}