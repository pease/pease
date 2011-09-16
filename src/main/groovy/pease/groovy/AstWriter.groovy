package pease.groovy

import org.codehaus.groovy.syntax.Types
import org.objectweb.asm.Opcodes
import pease.support.SpockTestClass
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*

// TODO replace CodeVisitorSupport with GroovyCodeVisitor when the whole feature set has been implemented.
// TODO make this class way shorter, to keep it maintainable

// Explanation of the SuppressWarnings annotation:
// ----------------------------------------
//   DuplicateStringLiteral: allowed due to multiple uses of small strings like ', ' that would look ugly when extracted into CONSTANTS
//   MethodCount: Implementing the visitor pattern leads to a massive amount of methods than can not be split up into various classes.
//   StatelessClass: CodeNarc thinks that this class is annotated with @Stateless, which is not true (Bug?)
@SuppressWarnings(['MethodCount', 'StatelessClass', 'DuplicateStringLiteral'])
@SpockTestClass('AstWriterSpec')
class AstWriter extends CodeVisitorSupport implements GroovyClassVisitor {

  private final StringBuffer STRING_BUILDER
  private final int INDENT_STEP = 2

  private int indent = 0
  private boolean indentExpressions = true
  private boolean wrapExpressions = true
  private boolean indentBlock = true
  private boolean wrapBlock = true

  void wrapExpressionIfEnabled() {
    if (wrapExpressions) {
      wrapLine()
    }
  }

  void wrapLine() {
    STRING_BUILDER.append '\n'
  }

  void indentExpressionIfEnabled() {
    if (indentExpressions) {
      STRING_BUILDER.append indent()
    }
  }

  void dontWrapAndIndentBlock(Closure c) {
    def beforeIndent = indentBlock
    def beforeWrap = wrapBlock

    indentBlock = wrapBlock = false
    c.call()

    indentBlock = beforeIndent
    wrapBlock = beforeWrap
  }

  void wrapAndIndentBlock(Closure c) {
    def beforeIndent = indentBlock
    def beforeWrap = wrapBlock

    indentBlock = wrapBlock = true
    c.call()

    indentBlock = beforeIndent
    wrapBlock = beforeWrap
  }

  void dontWrapAndIndentStatement(Closure c) {
    def beforeIndent = indentExpressions
    def beforeWrap = wrapExpressions

    indentExpressions = wrapExpressions = false
    c.call()

    indentExpressions = beforeIndent
    wrapExpressions = beforeWrap
  }

  void wrapAndIndentStatement(Closure c) {
    def beforeIndent = indentExpressions
    def beforeWrap = wrapExpressions

    indentExpressions()
    wrapExpressions()
    c.call()

    indentExpressions = beforeIndent
    wrapExpressions = beforeWrap
  }

  void indent(Closure c) {
    moreIndent()
    wrapAndIndentBlock {
      c.call()
    }
    lessIndent()
  }

  void wrapExpressions() {
    wrapExpressions = true
  }

  void dontWrapExpressions() {
    wrapExpressions = false
  }

  void indentExpressions() {
    indentExpressions = true
  }

  void dontIndentExpressions() {
    indentExpressions = false
  }

  String indent() {
    StringBuilder sb = new StringBuilder('')
    indent.times { sb.append(' ') }
    sb.toString()
  }

  void moreIndent() {
    indent += INDENT_STEP
  }

  void lessIndent() {
    assert indent >= INDENT_STEP
    indent -= INDENT_STEP
  }

  public AstWriter(StringBuffer sb) {
    this.STRING_BUILDER = sb
  }

  static String modifiersToString(int modifiers) {
    List<String> stringModifiers = []

    if (modifiers & Opcodes.ACC_PUBLIC) {
      stringModifiers << 'public'
    }

    if (modifiers & Opcodes.ACC_PRIVATE) {
      stringModifiers << 'private'
    }

    if (modifiers & Opcodes.ACC_STATIC) {
      stringModifiers << 'static'
    }

    if (modifiers & Opcodes.ACC_FINAL) {
      stringModifiers << 'final'
    }

    stringModifiers.join(' ')
  }

  void visitModuleNode(ModuleNode moduleNode) {
    for (ClassNode classNode: moduleNode.classes) {
      if (classNode.isScript()) {
        for (MethodNode method: moduleNode.methods) {
          visitMethod(method)
        }
      } else {
        visitClass(classNode)
      }
    }

    //visit Statements that are not inside a class
    if (!moduleNode.statementBlock.isEmpty()) {
      visitBlockStatement(moduleNode.statementBlock)
    }
  }


  void visitAnnotations(List<AnnotationNode> annotationNodes) {
    annotationNodes.each { AnnotationNode annotation ->
      STRING_BUILDER.append indent()
      visitAnnotationNode annotation
      STRING_BUILDER.append '\n'
    }
  }


  @Override
  void visitClass(ClassNode classNode) {
    visitAnnotations(classNode.annotations)

    STRING_BUILDER.append indent()

    def modifiers = modifiersToString(classNode.modifiers)
    if (modifiers) {
      STRING_BUILDER.append("$modifiers ")
    }

    STRING_BUILDER.append "class $classNode.name"

    if (classNode.superClass.name != 'java.lang.Object') {
      STRING_BUILDER.append " extends $classNode.superClass.name"
    }
    def interfaces = classNode.interfaces.grep { !(it.typeClass in [GroovyObject]) }*.name.join(', ')
    if (interfaces) {
      STRING_BUILDER.append " implements $interfaces"
    }
    STRING_BUILDER.append ' {\n'

    indent {
      classNode.innerClasses.toList().each { visitClass it }
      classNode.fields.each { visitField it }
      classNode.properties.each { visitProperty it }
      classNode.declaredConstructors.each { visitConstructorForClass it, classNode }
      classNode.methods.each { visitMethod(it) }
    }

    STRING_BUILDER.append indent() + '}\n'
  }

  void visitAnnotationNode(AnnotationNode annotationNode) {
    STRING_BUILDER.append "@${annotationNode.classNode.name}"

    if (annotationNode.members.size() > 0) {
      STRING_BUILDER.append '('
      annotationNode.members.sort {a, b -> a.key <=> b.key }.eachWithIndex { String str, Expression expr, idx ->
        STRING_BUILDER.append "$str="
        expr.visit this

        if (idx + 1 < annotationNode.members.size()) {
          STRING_BUILDER.append ', '
        }
      }
      STRING_BUILDER.append ')'
    }
  }

  @Override
  void visitConstructor(ConstructorNode node) {
    throw new UnsupportedOperationException('cannot be called direct use visitConstructorForClass instead')
  }

  void visitConstructorForClass(ConstructorNode constructorNode, ClassNode classNode) {
    indentExpressionIfEnabled()

    STRING_BUILDER.with {
      append modifiersToString(constructorNode.modifiers)
      append " $classNode.name"
      append '('
      append constructorNode.parameters.collect { "$it.type $it.name" }.join(', ')
      append ') '
    }

    dontWrapAndIndentBlock {
      constructorNode.code.visit this
    }

    wrapExpressionIfEnabled()
  }

  @Override
  void visitMethod(MethodNode node) {
    node.annotations.each() { AnnotationNode ann ->
      STRING_BUILDER.append indent()
      visitAnnotationNode(ann)
      STRING_BUILDER.append '\n'
    }

    STRING_BUILDER.append indent()

    def modifiers = modifiersToString(node.modifiers)
    if (modifiers) {
      STRING_BUILDER.append "$modifiers "
    }
    STRING_BUILDER.append "$node.returnType.name "

    // XXX: hack for Spock like method declarations
    if (node.name.contains(' ')) {
      STRING_BUILDER.append "\"$node.name\""
    } else {
      STRING_BUILDER.append "$node.name"
    }

    STRING_BUILDER.append '('
    STRING_BUILDER.append parameterToString(node.parameters)
    STRING_BUILDER.append ') '

    dontWrapAndIndentBlock {
      if (node.code != null) {
        node.code.visit this
      }
    }

    if (node.code?.class == BlockStatement) {
      wrapLine()
    }
  }

  static String parameterToString(Parameter[] parameters) {
    List params = []

    parameters.each { param ->
      params << "$param.type.name $param.name"
    }

    params.join(', ')
  }

  @Override
  void visitField(FieldNode node) {
    node.annotations.eachWithIndex { annotation, idx ->
      STRING_BUILDER.append indent()
      visitAnnotationNode annotation
      STRING_BUILDER.append '\n'
    }

    STRING_BUILDER.append indent()
    STRING_BUILDER.append "$node.type.name $node.name"
    STRING_BUILDER.append '\n'
  }

  // PropertyNode contain the fields that get "later" getter and setter methods.
  // There is no need to write a string representation of them.

  @Override
  void visitProperty(PropertyNode node) {
  }

  @Override
  void visitCastExpression(CastExpression expression) {
    STRING_BUILDER.append '('
    super.visitCastExpression(expression)
    STRING_BUILDER.append " as $expression.type.name)"
  }

  @Override
  void visitConstantExpression(ConstantExpression expr) {
    if (expr.value == null) {
      STRING_BUILDER.append 'null'
    } else {
      if (expr?.value?.class == String) { STRING_BUILDER.append '\'' }
      STRING_BUILDER.append expr.value
      if (expr?.value?.class == String) { STRING_BUILDER.append '\'' }
    }
  }

  @Override
  void visitReturnStatement(ReturnStatement stmt) {
    dontWrapAndIndentStatement {
      STRING_BUILDER.append indent() + 'return '
      stmt.expression.visit this
      STRING_BUILDER.append '\n'
    }
  }

  @Override
  void visitIfElse(IfStatement stmt) {
    STRING_BUILDER.append indent()

    dontWrapAndIndentStatement {
      STRING_BUILDER.append 'if ('
      stmt.booleanExpression.visit this
      STRING_BUILDER.append ') '
    }

    dontWrapAndIndentBlock {
      stmt.ifBlock.visit this
    }

    if (stmt.elseBlock instanceof EmptyStatement) {
      // dispatching to EmptyStatement will not call back visitor,
      // must call our visitEmptyStatement explicitly
      visitEmptyStatement((EmptyStatement) stmt.elseBlock)
      STRING_BUILDER.append '\n'
    } else {
      STRING_BUILDER.append ' else '
      stmt.elseBlock.visit(this)
    }
  }

  @Override
  void visitBlockStatement(BlockStatement block) {
    if (indentBlock) {
      STRING_BUILDER.append indent()
    }

    STRING_BUILDER.append '{\n'
    indent {
      block.statements.each { it.visit super }
    }
    STRING_BUILDER.append indent() + '}'

    if (wrapBlock) {
      STRING_BUILDER.append '\n'
    }
  }

  @Override
  void visitBinaryExpression(BinaryExpression expression) {
    indentExpressionIfEnabled()

    expression.leftExpression.visit this

    if (expression.operation.type == Types.LEFT_SQUARE_BRACKET) {
      STRING_BUILDER.append '['
      expression.rightExpression.visit this
      STRING_BUILDER.append ']'
    } else {
      STRING_BUILDER.append " $expression.operation.text "
      expression.rightExpression.visit this

    }

    wrapExpressionIfEnabled()
  }

  @Override
  void visitListExpression(ListExpression expr) {
    STRING_BUILDER.append '['
    def iter = expr.expressions.iterator()
    while (iter.hasNext()) {
      iter.next().visit(this)
      if (iter.hasNext()) {
        STRING_BUILDER.append ', '
      }
    }
    STRING_BUILDER.append ']'
  }

  @Override
  void visitRangeExpression(RangeExpression expression) {
    STRING_BUILDER.append expression.text
  }

  @Override
  void visitMethodCallExpression(MethodCallExpression call) {
    indentExpressionIfEnabled()

    call.objectExpression.visit this

    if (call.isSpreadSafe()) {
      STRING_BUILDER.append '*'
    }
    STRING_BUILDER.append '.'
    if (call.method instanceof ConstantExpression) {
      def c = (ConstantExpression) call.method
      STRING_BUILDER.append c.value
    } else {
      call.method.visit this
    }

    dontWrapAndIndentBlock {
      dontWrapAndIndentStatement {
        STRING_BUILDER.append '('
        call.arguments.visit(this)
        STRING_BUILDER.append ')'
      }
    }

    wrapExpressionIfEnabled()
  }

  @Override
  void visitVariableExpression(VariableExpression expression) {
    STRING_BUILDER.append expression.name
  }


  @Override
  void visitDeclarationExpression(DeclarationExpression expression) {
    dontWrapAndIndentBlock {
      dontWrapAndIndentStatement {
        STRING_BUILDER.append "$expression.leftExpression.type.name "

        if (expression.leftExpression instanceof TupleExpression) {
          STRING_BUILDER.append '('
          expression.leftExpression.visit this
          STRING_BUILDER.append ')'
        } else {
          expression.leftExpression.visit this
        }

        STRING_BUILDER.append " $expression.operation.text "

        expression.rightExpression.visit this
      }
    }

    wrapExpressionIfEnabled()
  }

  @Override
  void visitExpressionStatement(ExpressionStatement statement) {
    indentExpressionIfEnabled()

    if (statement.statementLabel != null) {
      STRING_BUILDER.append "$statement.statementLabel: "
    }

    dontWrapAndIndentStatement {
      statement.expression.visit this
    }

    wrapExpressionIfEnabled()
  }

  @Override
  void visitGStringExpression(GStringExpression expression) {
    STRING_BUILDER.append "\"$expression.text\""
  }

  @Override
  void visitAssertStatement(AssertStatement statement) {
    STRING_BUILDER.append indent()
    STRING_BUILDER.append 'assert '

    dontWrapAndIndentStatement {
      statement.booleanExpression.visit this

      if (statement.messageExpression.class == ConstantExpression && statement.messageExpression.value != null) {
        STRING_BUILDER.append ', '
        statement.messageExpression.visit this
      }
    }

    wrapLine()
  }

  @Override
  void visitTupleExpression(TupleExpression expression) {
    Iterator iter = expression.expressions.iterator()
    while (iter.hasNext()) {
      iter.next().visit this
      if (iter.hasNext()) {
        STRING_BUILDER.append ', '
      }
    }
  }

  @Override
  void visitSwitch(SwitchStatement statement) {
    STRING_BUILDER.append indent()

    STRING_BUILDER.append 'switch ('
    statement.expression.visit(this)
    STRING_BUILDER.append ') {\n'

    indent {
      statement.caseStatements.each { it.visit this }

      STRING_BUILDER.append indent() + 'default:'
      if (statement.defaultStatement.class == BlockStatement) {
        STRING_BUILDER.append '\n'
        moreIndent()
        statement.defaultStatement.statements.each { it.visit this }
        lessIndent()
      } else {
        STRING_BUILDER.append ' '
        statement.defaultStatement.visit this
      }
    }

    STRING_BUILDER.append indent() + '}\n'
  }

  @Override
  void visitCaseStatement(CaseStatement statement) {
    STRING_BUILDER.append indent()
    STRING_BUILDER.append 'case '
    statement.expression.visit this
    STRING_BUILDER.append ':'

    if (statement.code.class == BlockStatement) {
      STRING_BUILDER.append '\n'
      indent {
        statement.code.statements.each { it.visit this }
        STRING_BUILDER.append indent() + 'break\n'
      }
    } else {
      STRING_BUILDER.append ' '
      statement.code.visit this
    }
  }

  @Override
  void visitBitwiseNegationExpression(BitwiseNegationExpression expression) {
    STRING_BUILDER.append '~'
    expression.expression.visit this
  }

  @Override
  void visitForLoop(ForStatement forLoop) {
    STRING_BUILDER.append indent()

    dontWrapAndIndentStatement {
      STRING_BUILDER.append 'for '
      forLoop.collectionExpression.visit this
    }

    STRING_BUILDER.append ' '

    dontWrapAndIndentBlock {
      forLoop.loopBlock.visit this
    }

    if (forLoop.loopBlock.class == BlockStatement) {
      STRING_BUILDER.append '\n'
    }
  }

  @Override
  void visitPrefixExpression(PrefixExpression expression) {
    STRING_BUILDER.append expression.operation.text
    expression.expression.visit this
  }

  @Override
  void visitClosureListExpression(ClosureListExpression cle) {
    STRING_BUILDER.append '('
    Iterator iter = cle.expressions.iterator()
    while (iter.hasNext()) {
      iter.next().visit this
      if (iter.hasNext()) {
        STRING_BUILDER.append '; '
      }
    }
    STRING_BUILDER.append ')'
  }

  @Override
  void visitConstructorCallExpression(ConstructorCallExpression call) {
    STRING_BUILDER.with {
      if (call.isSuperCall()) {
        append 'super'
      } else if (call.isThisCall()) {
        append 'this'
      } else {
        append "new $call.type.name"
      }
    }

    STRING_BUILDER.append '('
    call.arguments.visit this
    STRING_BUILDER.append ')'

    wrapExpressionIfEnabled()
  }

  @Override
  void visitFieldExpression(FieldExpression expression) {
    STRING_BUILDER.append expression.field.name
  }


  @Override
  void visitNotExpression(NotExpression expression) {
    STRING_BUILDER.append '!'
    expression.expression.visit(this)
  }

  @Override
  void visitMapExpression(MapExpression expression) {
    STRING_BUILDER.append '['

    def iterator = expression.mapEntryExpressions.iterator()
    while (iterator.hasNext()) {
      def entry = iterator.next()
      entry.keyExpression.visit this
      STRING_BUILDER.append ': '
      entry.valueExpression.visit this

      if (iterator.hasNext()) {
        STRING_BUILDER.append ', '
      }
    }

    STRING_BUILDER.append ']'
  }

  @Override
  void visitTernaryExpression(TernaryExpression expression) {
    expression.booleanExpression.visit this
    STRING_BUILDER.append ' ? '

    expression.trueExpression.visit this
    STRING_BUILDER.append ' : '

    expression.falseExpression.visit this
  }

  @Override
  void visitClosureExpression(ClosureExpression expression) {
    if (expression.code.class == BlockStatement) {
      STRING_BUILDER.append '{'
      if (expression.parameters) {
        STRING_BUILDER.append ' '
      }
      STRING_BUILDER.append expression.parameters.collect { "$it.type.name $it.name" }.join(', ')
      STRING_BUILDER.append ' ->\n'
      wrapAndIndentStatement {
        indent {
          expression.code.statements.each { it.visit this }
        }
      }
      STRING_BUILDER.append indent() + '}'
    } else {
      expression.code.visit this
    }
  }

  @Override
  void visitPropertyExpression(PropertyExpression expression) {
    expression.objectExpression.visit this
    STRING_BUILDER.append '.'
    expression.property.visit this
  }

  @Override
  void visitClassExpression(ClassExpression expression) {
    STRING_BUILDER.append expression.text
  }
}
