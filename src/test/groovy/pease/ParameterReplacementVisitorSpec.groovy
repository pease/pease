package pease

import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import pease.groovy.AstWriter
import pease.groovy.ParameterReplacementVisitor
import spock.lang.Specification

class ParameterReplacementVisitorSpec extends Specification {
  def astBuilder = new AstBuilder()
  def visitor = new ParameterReplacementVisitor()

  // common statements, where the variable foo should be replaced
  def statements = astBuilder.buildFromString CompilePhase.SEMANTIC_ANALYSIS,
      '''
      def a = foo + "abc" // declaration(variable('a') , '=', binary(variable(foo), '+', constant('abc'))
      a << foo            // binary(variable('a'), '<<', variable('foo'))
      ['1', foo, '2']     // list(constant('1'), variable('foo'), constant('2'))
      (foo as int)        // cast(Integer, variable('foo'))
      methodCall(foo)     // return(methodCall(variable('this'), constant('methodCall'), argumentList(variable('foo'))))
  '''

  private visitStatementsAndBuildString(statements, visitor) {
    statements.each { it.visit visitor }

    def sb = new StringBuffer()
    new AstWriter(sb).visitBlockStatement((BlockStatement) statements[0])
    sb.toString()
  }

  def 'variable names should be replaced with other names'() {
    given:
    visitor.replacements['foo'] = new VariableExpression('bar')

    and:
    def expectedResult = '''\
    {
      java.lang.Object a = bar + 'abc'
      a << bar
      ['1', bar, '2']
      (bar as int)
      this.methodCall(bar)
    }
    '''.stripIndent()

    expect:
    visitStatementsAndBuildString(statements, visitor) == expectedResult
  }

  def 'variable names should be replaced with constants'() {
    given:
    visitor.replacements['foo'] = new ConstantExpression('5')

    and:
    def expectedResult = '''\
    {
      java.lang.Object a = '5' + 'abc'
      a << '5'
      ['1', '5', '2']
      ('5' as int)
      this.methodCall('5')
    }
    '''.stripIndent()

    expect:
    visitStatementsAndBuildString(statements, visitor) == expectedResult
  }

  def 'illegal statements should be recognized and reported instead of replaced'(String stmt) {
    given:
    visitor.replacements['foo'] = new ConstantExpression('5')

    and:
    def illegalStatements = astBuilder.buildFromString CompilePhase.SEMANTIC_ANALYSIS, stmt

    when:
    def result = visitStatementsAndBuildString(illegalStatements, visitor)

    then:
    result == null

    and:
    thrown(RuntimeException)

    where:
    stmt << ['def foo = 5', 'def (foo, bar) = [1,2]', 'foo++', '++foo', '(foo, a) = [1, 2]', 'foo = 2', 'foo *= 2', 'foo /= -1', 'foo += 1', 'foo -= 3']
  }
}