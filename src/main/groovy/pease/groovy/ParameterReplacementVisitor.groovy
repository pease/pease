package pease.groovy

import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import pease.support.SpockTestClass
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.Variable

@SpockTestClass('ParameterReplacementVisitorSpec')
class ParameterReplacementVisitor extends CodeVisitorSupport implements ExpressionTransformer {
  public final static INVALID_ASSIGNMENT = new RuntimeException('unable to replace variable assignment with constant')

  // this map stores which variable name (key) should be replaced with which other expression (value)
  Map<String, Expression> replacements = [:]

  Expression getReplacement(VariableExpression variableExpression) {
    replacements[variableExpression.name]
  }

  boolean expressionIsVariable(Expression expr) {
    expr instanceof Variable && ((Variable) expr).name in replacements?.keySet()
  }

  boolean isAssignmentExpression(BinaryExpression expr) {
    expr.operation.text in ['=', '*=', '+=', '-=', '/=']
  }

  boolean tupleContainsVariable(Expression expr) {
    expr instanceof TupleExpression && ((TupleExpression) expr).expressions.any { expressionIsVariable(it) }
  }

  @Override
  void visitDeclarationExpression(DeclarationExpression expression) {
    if (expression.isMultipleAssignmentDeclaration()) {
      TupleExpression tuple = (TupleExpression) expression.leftExpression
      tuple.expressions.each {
        if (expressionIsVariable(it)) {
          throw INVALID_ASSIGNMENT
        }
      }
    } else {
      if (expressionIsVariable(expression.variableExpression)) {
        throw INVALID_ASSIGNMENT
      }
    }

    super.visitDeclarationExpression(expression)
  }

  @Override
  void visitPrefixExpression(PrefixExpression expression) {
    if (expressionIsVariable(expression.expression)) {
      throw INVALID_ASSIGNMENT
    }

    super.visitPrefixExpression(expression)
  }

  @Override
  void visitPostfixExpression(PostfixExpression expression) {
    if (expressionIsVariable(expression.expression)) {
      throw INVALID_ASSIGNMENT
    }

    super.visitPostfixExpression(expression)
  }

  @Override
  void visitMethodCallExpression(MethodCallExpression call) {
    call.objectExpression = transform(call.objectExpression)
    call.arguments = call.arguments.transformExpression(this)

    super.visitMethodCallExpression(call)
  }

  @Override
  void visitBinaryExpression(BinaryExpression expr) {
    if (isAssignmentExpression(expr)) {
      if (expressionIsVariable(expr.leftExpression))
        throw INVALID_ASSIGNMENT
      else if (tupleContainsVariable(expr.leftExpression))
        throw INVALID_ASSIGNMENT
    }

    if (expressionIsVariable(expr.leftExpression)) {
      expr.leftExpression = transform(expr.leftExpression)
    } else {
      expr.leftExpression.visit(this)
    }

    if (expressionIsVariable(expr.rightExpression)) {
      expr.rightExpression = transform(expr.rightExpression)
    } else {
      expr.rightExpression.visit(this)
    }
  }

  @Override
  void visitExpressionStatement(ExpressionStatement statement) {
    statement.expression.visit(this)
    statement.expression = statement.expression.transformExpression(this)
  }

  @Override
  Expression transform(Expression expr) {
    expressionIsVariable(expr) ? getReplacement((VariableExpression) expr) : expr.transformExpression(this)
  }
}
