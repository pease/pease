package pease

import pease.gherkin.model.Tags
import pease.groovy.StepLoader
import pease.groovy.model.HookKind
import pease.support.SpecFileHelper
import spock.lang.Specification
import static pease.StepKind.*

@Mixin(SpecFileHelper)
class StepLoaderSpec extends Specification {
  StepLoader stepLoader = StepLoader.instance

  // TODO handle regex argument without NegationExpression in steps (eg normal strings)
  // TODO defined classes should be compiled
  def "load a step definition"() {
    given: 'there is a step definition'
    def steps = '''
            Given(~/I have a Given step/) {
            }

            When(~/I call a When block containing some test logic/) {
            }

            Then(~/I'm able to test in the Then block for results/) {
            }
        '''.stripIndent()

    when:
    def map = stepLoader.loadFromString(steps)

    then:
    map.steps.kind == [GIVEN, WHEN, THEN]

    and:
    map.steps.find { it.kind == GIVEN }.regex.pattern() == 'I have a Given step'

    and:
    map.steps.find { it.kind == WHEN }.regex.pattern() == 'I call a When block containing some test logic'

    and:
    map.steps.find { it.kind == THEN }.regex.pattern() == 'I\'m able to test in the Then block for results'
  }

  def 'world closures should be parsed'() {
    given:
    def steps = '''
      World(pease.support.MyCustomWorld)
    '''.stripIndent()

    when:
    def map = stepLoader.loadFromString(steps)

    then:
    map.worlds[0].worldClass.name == 'pease.support.MyCustomWorld'
  }

  def "global hooks should be parsed"() {
    given:
    def steps = '''
      Before {
         def a = 5
      }

      After {
        a = null
      }
    '''.stripIndent()

    when:
    def map = stepLoader.loadFromString(steps)

    then:
    map.hooks.kind == [HookKind.BEFORE, HookKind.AFTER]
  }

  def 'tagged hooks should be read'() {
    given:
    def steps = '''\
      Before('@single') {}
      Before('@tag1', '@anothertag') {}
      After('@myaftertag', '@aMixedCaseTag,@third_tag') {}
    '''.stripIndent()

    when:
    def loadedSteps = stepLoader.loadFromString(steps)

    then:
    loadedSteps.hooks.tags == [
      [new Tags(['@single'])], // Before('@si..')
      [new Tags(['@tag1']), new Tags(['@anothertag'])], // Before('@tag1', '@an..')
      [new Tags(['@myaftertag']), new Tags(['@aMixedCaseTag', '@third_tag'])] // After('@my..', '@aMix..,@thir..')
    ]

    and:
    loadedSteps.hooks.closureExpression.every { it != null }
  }

  // if this test case fails, check your IDE settings for exclusion of .groovy files from the resources
  // IDEA users should simply add '/**/step_definitions/?*.groovy' to their resource patterns
  def 'load from file'() {
    given:
    def stepsFile = '/step_definitions/simple.groovy'

    when:
    def loadedSteps = stepLoader.loadFromFile(getResourceFile(stepsFile))

    then:
    loadedSteps.steps.every { it.filename.endsWith stepsFile }

    and:
    loadedSteps.hooks.every { it.filename.endsWith stepsFile }

    and:
    loadedSteps.worlds.every { it.filename.endsWith stepsFile }

    and:
    loadedSteps.steps.regex*.pattern() as Set == [
        'I have this feature file',
        'I load it with pease',
        'it is syntactically valid'
    ] as Set
  }
}
