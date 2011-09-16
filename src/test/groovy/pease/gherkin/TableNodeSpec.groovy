package pease.gherkin

import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import pease.gherkin.model.TableNode
import spock.lang.Specification

class TableNodeSpec extends Specification {
  def 'a table should be created from a list'() {
    given:
    def table = TableNode.fromList([['a', 'b'], ['1', '2']])

    expect:
    table.rows.size() == 2

    and:
    table.rows.columns*.size() == [2, 2]
  }

  def 'a table should have a header'() {
    given:
    def table = TableNode.fromList([['header']])

    expect:
    table.header.columns == ['header']
  }

  def 'an AST returned from a header row should have valid identifier names'() {
    given:
    def table = TableNode.fromList([['a b', 'Huge']])

    when: 'generating a BinaryExpression from the header row'
    BinaryExpression expr = table.header.toBinaryExpression(false)

    then: 'the names should be valid identifiers'
    expr.leftExpression.variable == 'aB'
    expr.rightExpression.variable == 'huge'
  }

  def 'left associative BinaryExpression objects should be generated from a row'() {
    given:
    def table = TableNode.fromList([['a', 'b', 'c', 'd']])

    when:
    BinaryExpression expression = table.rows[0].toBinaryExpression(true)

    then: 'there is a left associative tree'
    expression.leftExpression.class == BinaryExpression
    expression.rightExpression.class == ConstantExpression

    and:
    BinaryExpression secondLevel = (BinaryExpression) expression.leftExpression
    secondLevel.leftExpression.class == BinaryExpression
    secondLevel.rightExpression.class == ConstantExpression

    and:
    BinaryExpression thirdLevel = (BinaryExpression) secondLevel.leftExpression
    thirdLevel.leftExpression.class == ConstantExpression
    thirdLevel.rightExpression.class == ConstantExpression
  }
}