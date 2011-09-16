package pease.gherkin.model

import groovy.transform.Immutable
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.syntax.Token
import pease.support.IdentifierConverter
import pease.support.SpockTestClass

@SpockTestClass('TableNodeSpec')
@Immutable
class RowNode {
  final static String OR_TOKEN = '|'
  List<String> columns = []

  def toListAst() {
    def list = new ListExpression()
    columns.each {
      list.addExpression(new ConstantExpression(it))
    }
    list
  }

  /** Wraps an iterator that normalizes the elements before returning them */
  class NormalizingIterator implements Iterator<String> {
    Iterator<String> inline

    public NormalizingIterator(Collection<String> collection) {
      inline = collection.iterator()
    }

    @Override
    boolean hasNext() {
      inline?.hasNext()
    }

    @Override
    String next() {
      IdentifierConverter.instance.toIdentifier(inline?.next())
    }

    @Override
    void remove() {
      inline?.remove()
    }
  }

  BinaryExpression toBinaryExpression(constantExpression = true) {
    assert columns.size() > 1

    def lastBinaryExpression = null
    def iter = constantExpression ? columns.iterator() : new NormalizingIterator(columns)

    while (iter.hasNext()) {
      def first = iter.next()

      if (lastBinaryExpression == null) {
        // we know that we have at least two cells
        def second = iter.next()

        lastBinaryExpression = new BinaryExpression(
            constantExpression ? new ConstantExpression(first) : new VariableExpression(first),
            Token.newString(OR_TOKEN, 0, 0),
            constantExpression ? new ConstantExpression(second) : new VariableExpression(second)
        )
      } else {
        lastBinaryExpression = new BinaryExpression(
            lastBinaryExpression,
            Token.newString(OR_TOKEN, 0, 0),
            constantExpression ? new ConstantExpression(first) : new VariableExpression(first)
        )
      }
    }

    lastBinaryExpression
  }
}
