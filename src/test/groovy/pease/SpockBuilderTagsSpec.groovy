package pease

import pease.support.AbstractSpockBuilderSpec

// Gherkin tags related tests
class SpockBuilderTagsSpec extends AbstractSpockBuilderSpec {
  def 'build only tagged scenarios'() {
    given:
    def feature = parseFeature '''\
        @globaltag
        Feature: My feature
          @unimportanttag
          Scenario: This scenario should be ignored

          @buildme
          Scenario: This scenario should be build
    '''.stripIndent()

    and:
    def tagExpression = ['@buildme']

    when:
    def specAst = spockBuilder.buildAST(feature, mkEmptySteps(), tagExpression).fromJust()

    then:
    specAst.methods.any { it.name == 'This scenario should be build' }

    and:
    specAst.methods.every { it.name != 'This scenario should be ignored' }
  }

  def 'feature tags are inherited by scenario nodes'() {
    given:
    def feature = parseFeature '''
      @buildme
      Feature: My feature
        Scenario: This scenario should be build
    '''.stripIndent()

    and:
    def tagExpression = ['@buildme']

    when:
    def specAst = spockBuilder.buildAST(feature, mkEmptySteps(), tagExpression).fromJust()

    then:
    specAst.methods.any { it.name == 'This scenario should be build' }
  }

  def 'compile only tag selected scenarios'() {
    given:
    def feature = parseFeature '''
      Feature: a simple feature

        Scenario: skip this scenario
          When this skipped scenario is executed
          Then the runtime flag should be false
          And the test should fail

        @important
        Scenario: build this scenario
          When this build scenario is executed
          Then the runtime flag should be true

        Scenario: also skip this scenario
          When this skipped scenario is executed
          Then the runtime flag should be false
          And the test should fail
    '''.stripIndent()

    and:
    def steps = parseSteps '''
      import pease.support.*

      World(MyCustomWorld)
      When(~/this (\\w+) scenario is executed/) { type ->
        state = (type == 'build') ? true : false
      }
      Then(~/the runtime flag should be (\\w+)/) { value ->
        state == value.toBoolean()
      }
      Then(~/the test should fail/) {
        assert false
      }
    '''.stripIndent()

    and:
    def spec = spockBuilder.compile(feature, steps, ['@important']).fromJust()

    when:
    def (sputnik, result) = runSpecWithSpySputnikRunner(spec)

    then:
    result.failures == []

    and:
    testWasExecuted(sputnik, 'state')
  }

  def 'dont build a feature if it is not tagged with the required tag'() {
    given:
    def feature = parseFeature '''
      @unimportant
      Feature: I am a skipped feature
        Scenario: I am also skipped
    '''.stripIndent()

    and:
    def tagExpression = ['@important']

    when:
    def maybeAst = spockBuilder.buildAST(feature, mkEmptySteps(), tagExpression)

    then:
    maybeAst.isNothing()

    when:
    def maybeSpec = spockBuilder.compile(feature, mkEmptySteps(), tagExpression)

    then:
    maybeSpec.isNothing()
  }

}