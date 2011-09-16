package pease

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import pease.groovy.StepLoader
import spock.lang.Specification

class StepLoaderFailuresSpec extends Specification {
  StepLoader stepLoader = StepLoader.instance

  def "syntax error"() {
    given:
    def stepWithSyntaxError = """\
        Hello, I'm not a groovy source code. Sorry for that!
    """.stripIndent()

    when:
    stepLoader.loadFromString(stepWithSyntaxError)

    then:
    thrown(MultipleCompilationErrorsException)
  }

  def "other method call"() {
    given: 'there is a step definition that contains a method call to an unexpected dsl method'
    def steps = '''
        GegebenSei(~/Ich schreibe auf Deutsch/) {
        }
    '''.stripIndent()

    when: 'the step file gets loaded'
    def map = stepLoader.loadFromString(steps)

    then: 'no step is loaded and the wrong step is ignored'
    map.steps.size() == 0
  }

  def 'other expression with a step definition'() {
    given: 'a step definition contains other Expressions that MethodCallExpressions'
    def steps = '''\
        Given(~/a valid method call/) {
        }

        1 + 2 ==
        3 // unexpected expression
    '''.stripIndent()

    when:
    stepLoader.loadFromString(steps)

    then: 'the step definition gets loaded without errors'
    notThrown(RuntimeException)
  }

}