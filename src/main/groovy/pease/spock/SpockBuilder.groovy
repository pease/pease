package pease.spock

import org.codehaus.groovy.ast.ClassNode
import pease.gherkin.SpockGenerationVisitor
import pease.gherkin.model.FeatureNode
import pease.groovy.AstWriter
import pease.groovy.model.StepDefinitionTree
import pease.support.Maybe
import pease.support.Maybe.Just
import pease.support.Maybe.Nothing
import pease.support.SpockTestClass

@Singleton
@SpockTestClass('SpockBuilderSpec')
class SpockBuilder {
  Maybe<ClassNode> buildAST(FeatureNode feature, StepDefinitionTree stepDefinitions, List<String> tagExpressions = []) {
    def visitor = new SpockGenerationVisitor(stepDefinitions, tagExpressions)

    feature.visit(visitor)

    visitor.resultingClassNode == null ? Nothing.instance : new Just(visitor.resultingClassNode)
  }

  Maybe<Class> compile(FeatureNode feature, StepDefinitionTree stepDefinitions, List<String> tagExpressions = [], Map parameter = [:]) {
    // This "AST to String" step is necessary,
    // because there may be errors when compiling the code.
    // In a perfect world, the abstract syntax tree would be compiled directly.
    //
    // Sadly this isn't a perfect world:
    //
    // The Groovy compiler must be able to point to the errors location and
    // the user must be able to understand and correct errors.
    // Without the generated source code, there would be too much magic
    // happening and the user would not be able to understand the errors.
    //
    // There is also a technical limitation that forces this step:
    // Groovy ASTTransformation instances are only executed as SourceUnit operation
    // When directly injecting the AST into the compiler using addClassNode, this
    // is only possible, when declaring an CompileUnit (and not a SourceUnit). Due to
    // this limitation, the Spock ASTTransformation would not be invoked.
    buildAST(feature, stepDefinitions, tagExpressions) >>> { ast ->
      Class clazz = (this.&compileCode << this.&astToString) ast
      new Just(clazz)
    }
  }

  String astToString(ClassNode classNode) {
    StringBuffer stringBuffer = new StringBuffer()
    AstWriter astWriter = new AstWriter(stringBuffer)
    astWriter.visitClass classNode

    stringBuffer.toString()
  }

  Class compileCode(String source) {
    // TODO Generate a reasonable filename from the feature filename.
    new GroovyClassLoader().parseClass(source, 'foo.groovy')
  }

}
