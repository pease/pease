package pease.groovy

import groovy.transform.TupleConstructor
import java.util.regex.Pattern
import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.control.ErrorCollector
import pease.StepKind
import pease.gherkin.model.Tags
import org.codehaus.groovy.ast.expr.*
import pease.groovy.model.*

@TupleConstructor
class BuildStepDefinitionTreeVisitor extends CodeVisitorSupport {
  ErrorCollector errorCollector

  final static STEP_METHODS = ['Given', 'When', 'Then'] as Set
  final static HOOK_METHODS = ['Before', 'After'] as Set
  final static WORLD_METHOD = 'World'

  def stepDefinitionTree = new StepDefinitionTree()

  List<Tags> extractTags(Range range, List<Expression> expressions) {
    expressions[range].value.collect { String val ->
      new Tags(val.split(',') as List<String>)
    }
  }

  // TODO this method should use the Maybe type
  def extractMethodCall(MethodCallExpression methodCallExpression) {
    TupleExpression tupleExpression = (TupleExpression) methodCallExpression.arguments
    def regex = null
    def closure = null

    switch (tupleExpression.expressions.size()) {
      case 1:
        closure = tupleExpression.expressions[0]
        break

      case 2:
        regex = tupleExpression.expressions[0].expression.value
        closure = tupleExpression.expressions[1]
        break

      default: // TODO insert error handling code here
        false
    }

    [regex, closure]
  }

  ClassExpression extractWorldMethodCall(MethodCallExpression methodCallExpression) {
    methodCallExpression.arguments.expressions[0]
  }

  @SuppressWarnings(['DuplicateNumberLiteral']) // the duplicate occurrences of '2' are valid
  def extractHookMethodCall(MethodCallExpression methodCallExpression) {
    def closure
    List<Tags> tags = []
    ArgumentListExpression ale = (ArgumentListExpression) methodCallExpression.arguments

    switch (ale.expressions.size()) {
      case 1: // hook call without tags aka global hooks
        closure = ale.expressions[0]
        break

      case { it >= 2 }: // hook call with tags (tagged hook)
        tags += extractTags(0..ale.expressions.size() - 2, ale.expressions)
        closure = ale.expressions[ale.expressions.size() - 1]
        break
    }

    [tags, closure]
  }

  /* TODO how to handle nested org.codehaus.groovy.ast.expr.MethodCallExpression? Maybe a visitor is not the right way
     to approach this problem ... */
  @Override
  void visitMethodCallExpression(MethodCallExpression call) {
    String name = call.methodAsString

    switch (name) {
      case { it in STEP_METHODS }:
        def (regex, closure) = extractMethodCall(call)
        stepDefinitionTree.steps << new StepDefinitionNode(StepKind.fromString(name), Pattern.compile(regex), (ClosureExpression) closure)
        break

      case { it in HOOK_METHODS }:
        def (tags, closure) = extractHookMethodCall(call)
        stepDefinitionTree.hooks << new HookNode(name.toUpperCase() as HookKind, closure, tags)
        break

      case WORLD_METHOD:
        def closure = extractWorldMethodCall(call)
        // the closure name does not fit here
        if (closure.class == ClassExpression) {
          stepDefinitionTree.worlds << new WorldNode(closure.type)
        }
        break
    }
  }
}
