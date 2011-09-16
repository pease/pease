package pease.groovy

import groovy.transform.TupleConstructor
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.GroovyCodeVisitor
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.ErrorCollector
import pease.FileLoader
import pease.groovy.model.StepDefinitionTree
import pease.support.SpockTestClass

/**
 * Provides methods for loading a step definition file and returning a map with its contents.
 */
@SpockTestClass('StepLoaderSpec')
@Singleton
@TupleConstructor
class StepLoader {
  private StepLoader() {}

  final static STEP_METHODS = ['Given', 'When', 'Then'] as Set
  final static HOOK_METHODS = ['Before', 'After'] as Set
  final static WORLD_METHOD = 'World'

  // position of the BlockStatement that represents the script in the Groovy AST
  private final static BLOCK_STATEMENT_INDEX = 0

  final ErrorCollector errorCollector = new ErrorCollector(CompilerConfiguration.DEFAULT)

  StepDefinitionTree loadFromString(String sourceCode) throws RuntimeException {
    List<ASTNode> nodes = buildASTFromString(sourceCode)

    GroovyCodeVisitor extractVisitor = new BuildStepDefinitionTreeVisitor(errorCollector)
    nodes[BLOCK_STATEMENT_INDEX].visit(extractVisitor)

    extractVisitor.stepDefinitionTree
  }

  StepDefinitionTree loadFromFile(String filename) {
    def file = new File(filename)
    def tree = loadFromString(file.text)

    tree.steps.each { it.filename = filename }
    tree.hooks.each { it.filename = filename }
    tree.worlds.each { it.filename = filename }

    tree
  }

  StepDefinitionTree loadSteps(String directory) {
    StepDefinitionTree result = new StepDefinitionTree()

    for (step in FileLoader.instance.findSteps(directory)) {
      def tree = loadFromFile(step)
      result.merge(tree)
    }

    result
  }

  List<ASTNode> buildASTFromString(String sourceCode) {
    def astBuilder = new AstBuilder()

    astBuilder.buildFromString CompilePhase.CANONICALIZATION, sourceCode
  }
}
