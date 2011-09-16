package pease.groovy.model

import pease.support.IdentifierConverter
import pease.support.Maybe
import pease.support.Maybe.Just
import pease.support.Maybe.Nothing
import pease.support.SpockTestClass
import pease.gherkin.model.*

@SpockTestClass('StepDefinitionTreeSpec')
class StepDefinitionTree {
  final List<StepDefinitionNode> steps = []
  final List<HookNode> hooks = []
  final List<WorldNode> worlds = []

  // Regex matching strategy: First match wins
  Maybe<ConcreteStep> searchTreeAndBuildMatches(StepNode stepNode, Closure<StepParam> paramBuilder) {
    def extraParams = []

    if (stepNode.table) {
      extraParams << new TableParam(stepNode.table)
    }
    if (stepNode.docString) {
      extraParams << new ValueParam(stepNode.docString)
    }

    def stepIterator = steps.iterator()
    def result = Nothing.instance
    while (result.isNothing() && stepIterator.hasNext()) {
      def stepCode = stepIterator.next()

      result = stepCode.matchStepNode(stepNode) >>> { groups ->
        List params = groups.collect(paramBuilder) + extraParams
        new Just(new ConcreteStep(stepCode, params))
      }
    }

    result
  }

  Maybe<ConcreteStep> findConcreteStep(StepNode node) {
    Closure<StepParam> buildParams = { name, start, end -> new ValueParam(name) } as Closure<StepParam>

    searchTreeAndBuildMatches(node, buildParams)
  }

  Maybe<ConcreteStep> findConcreteStep(ConcreteTemplateStepNode node) {
    Closure<StepParam> buildVariableOrValueParam = { text, start, end ->
      def variable = node.variablePositions.find { it.startPosition == start && it.endPosition == end }
      if (variable) {
        new VariableParam(IdentifierConverter.instance.toIdentifier(variable.variableName))
      } else {
        new ValueParam(text)
      }
    }

    searchTreeAndBuildMatches(node, buildVariableOrValueParam)
  }

  Maybe<ConcreteStep> findConcreteStep(TemplateStepNode templateStepNode, TableNode examples) {
    List stepList = examples?.collectWithSubTable { subTable ->
      templateStepNode.makeConcreteTemplateStepNode(subTable) >>> {  ConcreteTemplateStepNode node ->
        findConcreteStep(node)
      }
    }

    // check if all ConcreteStep instances are the same
    stepList?.inject(stepList?.getAt(0)) { Maybe rest, Maybe val ->
      rest.isJust() && rest.fromJust() == val.fromJust() ? rest : Nothing.instance
    } ?: Nothing.instance
  }

  StepDefinitionTree merge(StepDefinitionTree stepDefinitionTree) {
    steps.addAll(stepDefinitionTree.steps)
    hooks.addAll(stepDefinitionTree.hooks)
    worlds.addAll(stepDefinitionTree.worlds)

    this
  }

  List<SpockBlock> findGlobalHooks(HookKind kind) {
    hooks.findAll { it.kind == kind && it.tags.empty }.collect {
      new SpockBlock(it.closureExpression.code.statements)
    }
  }

  List<SpockBlock> findLocalHooks(HookKind kind, Tags tags) {
    hooks.findAll { it.kind == kind && it.matches(tags) }.collect {
      new SpockBlock(it.closureExpression.code.statements)
    }
  }

  List<SpockBlock> findBeforeHook(Tags tags) {
    findGlobalHooks(HookKind.BEFORE) +
        findLocalHooks(HookKind.BEFORE, tags)
  }

  List<SpockBlock> findAfterHook(Tags tags) {
    findGlobalHooks(HookKind.AFTER) +
        findLocalHooks(HookKind.AFTER, tags)
  }
}
