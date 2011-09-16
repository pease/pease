package pease.gherkin.model

import groovy.transform.Immutable
import org.codehaus.groovy.ast.expr.ListExpression
import pease.support.SpockTestClass
import pease.gherkin.GherkinVisitor

// The first table node is the table header most of the time (see TableNode#getHeader)
@SpockTestClass('TableNodeSpec')
@Immutable
class TableNode extends GherkinNode {
  final List<RowNode> rows = []

  // build an AST representation of this TableNode in list form
  def toListAst() {
    def list = new ListExpression()

    rows.each {
      list.addExpression it.toListAst()
    }

    list
  }

  static fromList(List<List> list) {
    List<RowNode> rows = []

    for (List subList in list) {
      rows.add(new RowNode(columns: subList))
    }
    new TableNode(rows)
  }

  // get the table header  and the specified `row' from this table
  def getTableWithRow(int row) {
    assert row < rows.size() - 1

    new TableNode([rows.head()] + [rows.get(row + 1)])
  }

  def getHeader() {
    rows[0]
  }

  def hasRow(int rowNumber) {
    rowNumber < rows.size() - 1
  }

  def eachRowWithIndex(Closure c) {
    0.upto(rows.size() - 2, c)
  }

  def collectWithSubTable(Closure closure) {
    rows[1..-1].collect { row ->
      def subTable = [:]
      header.columns.eachWithIndex { col, index ->
        subTable[col] = row.columns[index]
      }

      closure.call(subTable)
    }
  }

  @Override
  void visit(GherkinVisitor visitor) {
    visitor.visitExamples(this)
  }
}
