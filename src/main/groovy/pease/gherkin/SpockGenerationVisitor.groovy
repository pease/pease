package pease.gherkin

import groovy.transform.TupleConstructor
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.spockframework.util.NotThreadSafe
import pease.StepKind
import pease.groovy.model.ConcreteStep
import pease.groovy.model.PendingSpockBlock
import pease.groovy.model.SpockBlock
import pease.groovy.model.StepDefinitionTree
import pease.spock.Report
import pease.support.SpockTestClass
import spock.lang.Ignore
import spock.lang.SpecName
import spock.lang.Specification
import org.codehaus.groovy.ast.*
import static org.objectweb.asm.Opcodes.ACC_PUBLIC
import pease.gherkin.model.*

@TupleConstructor
@NotThreadSafe
@SpockTestClass('SpockGenerationVisitorSpec')
class SpockGenerationVisitor extends GherkinVisitorSupport {
  StepDefinitionTree stepDefinitionTree
  List<String> tagExpressions

  ClassNode resultingClassNode
  List<MethodNode> methodNodes = []
  List<SpockBlock> blocks = []

  private StepKind lastBlockKind

  private String generateSpecClassName(featureFileName) {
    def name = null

    if (featureFileName?.size() > 1) {
      name = featureFileName[0].toUpperCase() + featureFileName[1..-1]
      name = isValidName(name) ? name : null
    }

    name ?: generateRandomName()
  }

  private long randomNr() {
    System.currentTimeMillis()
  }

  private String generateRandomName() {
    "SpockSpec${randomNr()}"
  }

  private boolean isValidName(String p) {
    p ==~ /[A-Z][A-Za-z0-9_]*/
  }

  def getFileName(String filePath) {
    filePath?.split('/')?.last()?.split('\\.')?.first()
  }

  private ClassNode createClassNode(String className, String featureName) {
    ClassNode classNode = new ClassNode(className, ACC_PUBLIC, ClassHelper.make(Specification))
    classNode.addInterface(ClassHelper.make(GroovyObject))

    // use a custom 'world' if present
    // TODO handle tagged worlds
    if (stepDefinitionTree.worlds.size() > 0) {
      // TODO issue a warning message when multiple worlds are defined
      classNode.superClass = stepDefinitionTree.worlds[0].worldClass
    }

    // Annotate the ClassNode with the name of the FeatureNode (@SpecName("..."))
    def specNameAnnotation = new AnnotationNode(ClassHelper.make(SpecName))
    specNameAnnotation.addMember('value', new ConstantExpression(featureName))
    classNode.addAnnotation(specNameAnnotation)

    // The Report annotation triggers the ReportExtension Spock extension
    classNode.addAnnotation(new AnnotationNode(ClassHelper.make(Report)))

    classNode
  }

  private createLabeledExpression(label, text) {
    def constantExpression = new ConstantExpression(text)
    def labeledExpression = new ExpressionStatement(constantExpression)
    labeledExpression.statementLabel = label

    labeledExpression
  }

  MethodNode createMethodNode(String methodName, List<SpockBlock> blocks) {
    def methodNode = new MethodNode(methodName, ACC_PUBLIC, ClassHelper.VOID_TYPE,
        Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, null)

    BlockStatement methodCode = new BlockStatement()
    blocks.each { SpockBlock block ->
      methodCode.addStatements(block.statements)
    }
    methodNode.code = methodCode

    if (blocks.any { it instanceof PendingSpockBlock }) {
      def ignore = new AnnotationNode(ClassHelper.make(Ignore))
      ignore.addMember('value', new ConstantExpression('pending blocks'))

      methodNode.addAnnotation(ignore)
    }

    methodNode
  }

  SpockBlock createBlock(StepKind lastKind, StepNode stepNode, ConcreteStep concreteStep) {
    List<Statement> statements = []

    statements << createLabeledExpression(lastKind == stepNode.kind ? 'and' : stepNode.kind.label, stepNode.text)
    statements << createFileOriginStatement("${concreteStep.filename ?: ''}:${concreteStep.closureExpression.lineNumber}")
    statements += concreteStep.rewriteClosure()

    new SpockBlock(statements)
  }

  SpockBlock createPendingBlock(StepKind lastKind, StepNode stepNode) {
    List<Statement> statements = []

    statements << createLabeledExpression(lastKind == stepNode.kind ? 'and' : stepNode.kind.label, stepNode.text)
    statements << createFileOriginStatement('pending')

    new PendingSpockBlock(statements)
  }

  Statement createFileOriginStatement(String s) {
    new ExpressionStatement(new ConstantExpression('file:' + s))
  }

  SpockBlock createWhereBlock(TableNode tableNode) {
    def statements = []

    statements.add(createLabeledExpression('where', ''))

    def firstRow = true
    tableNode.rows.each { RowNode row ->
      statements.add(new ExpressionStatement(row.toBinaryExpression(!firstRow)))
      firstRow = false
    }

    new SpockBlock(statements)
  }

  @Override
  void visitFeatureNode(FeatureNode featureNode) {
    super.visitFeatureNode(featureNode)

    // if there are methods for the class, than generate the class
    if (methodNodes.size() > 0) {
      def className = generateSpecClassName(getFileName(featureNode.fileName))
      resultingClassNode = createClassNode(className, featureNode.name)

      methodNodes.each { method ->
        resultingClassNode.addMethod(method)
      }
    }

    // TODO maybe issue a warning when an empty class would result
  }

  @Override
  void visitScenarioNode(ScenarioNode scenarioNode) {
    if (scenarioNode.tags.matches(tagExpressions)) {
      blocks = []
      lastBlockKind = null

      blocks += stepDefinitionTree.findBeforeHook(scenarioNode.tags)
      super.visitScenarioNode(scenarioNode)
      blocks += stepDefinitionTree.findAfterHook(scenarioNode.tags)

      methodNodes << createMethodNode(scenarioNode.name, blocks)
    }
  }

  @Override
  void visitStepNode(StepNode stepNode) {
    def maybeConcreteStep = stepDefinitionTree.findConcreteStep(stepNode)

    if (maybeConcreteStep.isJust()) {
      blocks << createBlock(lastBlockKind, stepNode, maybeConcreteStep.fromJust())
    } else {
      blocks << createPendingBlock(lastBlockKind, stepNode)
    }

    lastBlockKind = stepNode.kind
  }

  @Override
  void visitScenarioOutlineNode(ScenarioOutlineNode scenarioOutlineNode) {
    if (scenarioOutlineNode.tags.matches(tagExpressions)) {
      blocks = []
      lastBlockKind = null

      blocks += stepDefinitionTree.findBeforeHook(scenarioOutlineNode.tags)

      scenarioOutlineNode.steps.each { stepTemplate ->
        def concreteStep = stepDefinitionTree.findConcreteStep(stepTemplate, scenarioOutlineNode.examples)
        if (concreteStep.isJust()) {
          blocks << createBlock(lastBlockKind, stepTemplate.stepNode, concreteStep.fromJust())
          lastBlockKind = stepTemplate.kind
        } else {
          blocks << createPendingBlock(lastBlockKind, stepTemplate.stepNode)
        }
      }

      // TODO ScenarioOutlineNodes without example tables are semantically wrong, issue a warning/error
      if (scenarioOutlineNode.examples) {
        blocks << createWhereBlock(scenarioOutlineNode.examples)
      }

      blocks += stepDefinitionTree.findAfterHook(scenarioOutlineNode.tags)

      methodNodes << createMethodNode(scenarioOutlineNode.name, blocks)
    }
  }

  @Override
  void visitTemplateStepNode(TemplateStepNode stepNode) {
    // this method will never be called, because the algorithms are embedded in the visitScenarioOutlineNode method
    throw new IllegalStateException('This method is not implemented and should not be called')
  }
}
