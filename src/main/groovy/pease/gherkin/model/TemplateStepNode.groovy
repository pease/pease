package pease.gherkin.model

import groovy.transform.TupleConstructor
import pease.gherkin.GherkinVisitor
import pease.support.Maybe
import pease.support.Maybe.Just
import pease.support.Maybe.Nothing

// A StepNode that needs to be processed before it can be handled as StepNode (see `Scenario Outline')
@TupleConstructor
class TemplateStepNode extends GherkinNode {
  private static final String VAR_START = '<'
  private static final String VAR_END = '>'
  @Delegate StepNode stepNode

  // Replaces the template text with the variables in the `vars' map and returns a List of triples (encoded as List) that stats the order of variable
  // occurrence's and the start- and end-position of  the replaced variable placeholder
  Maybe<ConcreteTemplateStepNode> makeConcreteTemplateStepNode(Map<String, String> vars) {
    def newStringBuilder = new StringBuilder()
    def variableBuilder
    def startPos, endPos
    def varOrder = []
    // state variable that indicates whether a variable is currently being read or not
    def readVar = false
    // if anything illegal happens (such as nested variables), this will be true and the result `Nothing'
    def error = false

    text.each { chr ->
      if (chr == VAR_START) {
        if (readVar) {
          error = true
        } else {
          readVar = true
          startPos = newStringBuilder.length()
        }

        variableBuilder = new StringBuilder()
      } else if (chr == VAR_END) {
        if (readVar) {
          readVar = false
        } else {
          error = true
        }

        def varName = variableBuilder.toString()
        if (varName in vars.keySet()) {
          newStringBuilder.append vars[varName]

          endPos = newStringBuilder.length()

          // return a triple that is encoded as a List
          varOrder << new TemplateVariablePosition(startPos, endPos, varName)
        } else {
          // no match, so put the variable back into the string
          newStringBuilder.append "$VAR_START$varName$VAR_END"
        }
      } else {
        if (readVar) {
          variableBuilder.append chr
        } else {
          newStringBuilder.append chr
        }
      }
    }


    error ? Nothing.instance : new Just(new ConcreteTemplateStepNode(kind, newStringBuilder.toString(), table, docString, varOrder))
  }

   void visit(GherkinVisitor visitor) {
    visitor.visitTemplateStepNode(this)
  }
}
