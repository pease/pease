package pease.gherkin.model

import groovy.transform.TupleConstructor
import pease.Configuration
import pease.gherkin.GherkinVisitor
import pease.groovy.model.StepDefinitionTree
import pease.spock.SpockBuilder
import pease.support.Maybe
import pease.support.Maybe.Just

@TupleConstructor(includeSuperProperties=true)
class FeatureNode extends TaggedNode  {
  final String name
  final List<ScenarioOutlineNode> scenarioOutlines = []
  final List<ScenarioNode> scenarios = []
  String fileName
  def spockBuilder = SpockBuilder.instance

  // enable convenient dispatching on FeatureNode
  Maybe<Class> compile(StepDefinitionTree tree, Configuration configuration) {
    spockBuilder.compile(this, tree, configuration.tagExpression)
  }

  Maybe<String> compileSpock(StepDefinitionTree tree, Configuration configuration) {
    spockBuilder.buildAST(this, tree, configuration.tagExpression) >>> { ast ->  new Just(spockBuilder.astToString(ast)) }
  }

  void visit(GherkinVisitor visitor) {
    visitor.visitFeatureNode(this)
  }
}
