package pease.gherkin

import gherkin.lexer.LexingError
import pease.gherkin.model.FeatureNode
import pease.gherkin.model.ScenarioNode
import pease.gherkin.model.Tags
import pease.gherkin.model.TemplateStepNode
import pease.support.SpecFileHelper
import spock.lang.Specification
import static pease.StepKind.*

@Mixin(SpecFileHelper)
class FeatureLoaderSpec extends Specification {

  FeatureLoader featureLoader = FeatureLoader.instance

  def loadFeatureContent(String content) {
    featureLoader.loadFromString("""
      Feature: i dont care
        $content
    """).fromMaybe(null)
  }

  def 'the feature loader should be able to load a feature from a file'() {
    when:
    def feature = featureLoader.loadFromFile(getResourceFile('/Simple.feature')).fromMaybe(null)

    then:
    feature.fileName.endsWith('Simple.feature')

    and:
    feature.name == 'Specify the functionality of Pease'
  }

  def 'loading feature definitions with syntax errors should lead to a compiler exception'() {
    when: 'parsing a random string'
    featureLoader.loadFromString 'Im not a feature definition'

    then: 'an exception must be thrown'
    thrown LexingError
  }

  def 'the feature parser should build FeatureNode nodes'(String featureName, String name) {
    when: 'parsing the feature'
    FeatureNode feature = featureLoader.loadFromString(featureName).fromMaybe(null)

    then: 'the tree contains the feature'
    feature.name == name

    where:
    featureName                                          | name
    'Feature: Abc'                                       | 'Abc'
    'Feature: Long feature name with special chars éüß©' | 'Long feature name with special chars éüß©'
  }

  def 'scenarios should be recognized'() {
    when: 'parsing a feature file containing scenario entries'
    def feature = '''
      Feature: My first feature
        Scenario: First scenario
        Scenario: Second scenario
    '''
    FeatureNode ast = featureLoader.loadFromString(feature).fromMaybe(null)

    then: 'the feature node is present'
    ast.name == 'My first feature'

    and: 'the feature node contains the specified scenario nodes'
    ast.scenarios*.name == ['First scenario', 'Second scenario']
  }

  def 'tags should be recognized on feature and scenario nodes and inheritance should be resolved'() {
    given:
    def featureFile = '''
      @mytag @anothertag
      Feature: FeatureWithTag

        @myscenariotag @myothertag
        Scenario: Scenario with tag
    '''

    and:
    def expectedFeatureTags = ['@mytag', '@anothertag'] as Tags
    def expectedScenarioTags = expectedFeatureTags + ['@myscenariotag', '@myothertag'] as Tags

    when: 'parsing a feature with tags'
    FeatureNode feature = featureLoader.loadFromString(featureFile).fromMaybe(null)

    then: 'the resulting feature contains all the tags'
    feature.tags.containsAll expectedFeatureTags

    and:
    feature.scenarios.first().tags.containsAll expectedScenarioTags
  }

  def 'loading a feature file with only a scenario node should fails'() {
    when: 'parsing a file only containing a scenario and no feature'
    def features = featureLoader.loadFromString loadFileContents('/TopLevelScenario.feature')

    then: 'the result contains no features'
    features.isNothing()
  }

  def 'equals with null object should return false'() {
    when: 'calling equals with a null value parameter'
    def result = new ScenarioNode().equals(null)

    then: 'equals must be false'
    !result
  }


  def 'loading a feature file containing only second level elements should fail'(String content) {
    expect: featureLoader.loadFromString(content).isNothing()

    where:
    content << ['Scenario Outline: foo', 'Scenario: bar']
  }

  def 'steps should be parsed'() {
    when:
    def feature = featureLoader.loadFromString '''
      Feature: feature
        Scenario: scenario
          Given given
          When when
          Then then
    ''' fromMaybe(null)

    then:
    feature.scenarios[0].steps*.kind == [GIVEN, WHEN, THEN]

    and:
    feature.scenarios[0].steps*.text == ['given', 'when', 'then']
  }

  def 'step types that are connected with AND should be resolved'() {
    when:
    def feature = featureLoader.loadFromString '''
      Feature: feature

        Scenario: scenario
          Given given
          And and given
          When when
          And and when
          Then then
          And and then
    ''' fromMaybe(null)

    then:
    feature.scenarios[0].steps*.kind == [GIVEN, GIVEN, WHEN, WHEN, THEN, THEN]
  }

  def 'tables for steps should be recognized'() {
    when:
    def feature = featureLoader.loadFromString '''
      Feature: feature

        Scenario: scenario
          Given I have following users
            | user    | email                |
            | gcantor | george@cantor.de     |
            | leibniz | gottfried@leibniz.de |
            | hcurry  | haskell@curry.com    |
          When I filter those from Germany
          Then I should have the following users
            | user    | email                |
            | gcantor | george@cantor.de     |
            | leibniz | gottfried@leibniz.de |
    ''' fromMaybe(null)

    and:
    def firstTable = [
        ['user', 'email'],
        ['gcantor', 'george@cantor.de'],
        ['leibniz', 'gottfried@leibniz.de'],
        ['hcurry', 'haskell@curry.com']
    ]
    def secondTable = [
        ['user', 'email'],
        ['gcantor', 'george@cantor.de'],
        ['leibniz', 'gottfried@leibniz.de']
    ]

    then:
    def firstStep = feature.scenarios[0].steps[0]
    def thirdStep = feature.scenarios[0].steps[2]

    and:
    firstStep.table.rows*.columns == firstTable
    thirdStep.table.rows*.columns == secondTable
  }

  def 'scenario outlines should be parsed'() {
    when:
    def feature = featureLoader.loadFromString '''
      Feature: a feature

        Scenario Outline: my complex scenario
          Given I have a <foo bar>
          When I put the <foo bar> into my <container for foo bars>
          Then I should have one <foo bar> in my <container for foo bars>

          Examples:
            | foo bar            | container for foo bars |
            | cuke               | shoppingcart           |
            | piece of java code | editor                 |
    '''.stripIndent() fromMaybe(null)

    then:
    feature.scenarioOutlines.size() == 1

    and:
    feature.scenarioOutlines[0].steps.size() == 3

    and:
    feature.scenarioOutlines[0].steps[0].class == TemplateStepNode.class

    and:
    feature.scenarioOutlines[0].examples.rows.size() == 3
    feature.scenarioOutlines[0].examples.rows[0].columns == ['foo bar', 'container for foo bars']
  }

  def 'doc strings attached to steps should be parsed'() {
    given:
    def expectedDocString = '''\
      This is what a pysting argument looks like.
      It can have multiple lines and contain all types of content.
        Indention is also respected.'''.stripIndent()

    when:
    def feature = loadFeatureContent '''
      Scenario: a scenario
        When there is a step containing a pystring argument
          """
          This is what a pysting argument looks like.
          It can have multiple lines and contain all types of content.
            Indention is also respected.
          """
        Then the parser should be able to connect it the the right step
    '''

    then:
    feature.scenarios[0].steps[0].docString == expectedDocString
  }
}
