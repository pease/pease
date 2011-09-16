package pease.groovy

import org.codehaus.groovy.ast.stmt.BlockStatement
import pease.support.AbstractAstWriterTest
import org.codehaus.groovy.ast.expr.*

class AstWriterExpressionSpec extends AbstractAstWriterTest {
  def 'test small expressions'() {
    given:
    def nodes = buildAstFrom(expression).find { it.class == BlockStatement }.statements

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == "$expected\n"

    where:
    type                      | expression                       | expected
    BinaryExpression          | '((1 | 2) | 3) | 4'              | '1 | 2 | 3 | 4'
    ConstructorCallExpression | 'new String()'                   | 'new String()'
    NotExpression             | '!true'                          | '!true'
    MapExpression             | "['a':1,'b':2]"                  | "['a': 1, 'b': 2]"
    DeclarationExpression     | 'def a = 2'                      | 'java.lang.Object a = 2'
    MethodCallExpression      | "def a = ['aa', 'a']; a*.size()" | "java.lang.Object a = ['aa', 'a']\na*.size()"
    TernaryExpression         | 'def a = null; a ?: "foo"'       | "java.lang.Object a = null\na ? a : 'foo'"
    ClosureExpression         | '{ a, b -> a * b}'               | '{ java.lang.Object a, java.lang.Object b ->\n  a * b\n}'
    CastExpression            | /'2' as int/                     | /('2' as int)/
    BinaryExpression          | '[1, 2][1..-1]'                  | '[1, 2][(1..-1)]'
  }

}