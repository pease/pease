package pease

import pease.spock.ReportListener
import pease.support.CapturePrintStream
import spock.lang.Specification
import org.spockframework.runtime.model.*
import static org.spockframework.runtime.model.BlockKind.*

class SpockReportListenerSpec extends Specification {
  def stringWriter = new CapturePrintStream()
  def reportListener = new ReportListener(stringWriter)

  def makeFeatureInfo() {
    def featureInfo = new FeatureInfo()

    featureInfo.with {
      name = 'scenario'
      successful = true
      addBlock mkBlock(WHEN, ['when'], ['./features/step_definitions/foo.groovy:12'])
      addBlock mkBlock(THEN, ['then'], ['./features/step_definitions/foo.groovy:4'])
    }

    featureInfo
  }

  def makePendingFeatureInfo() {
    def featureInfo = makeFeatureInfo()

    featureInfo.skipped = true
    featureInfo.blocks[0].fileOrigins[0] = 'pending'

    featureInfo
  }

  def mkBlock(kind, texts, fileOrigins = []) {
    def blockInfo = new BlockInfo()
    blockInfo.kind = kind
    blockInfo.texts = texts
    blockInfo.fileOrigins = fileOrigins
    blockInfo
  }

  def "beforeSpec prints the name of the gherkin-feature and increases the indent"() {
    given:
    def specInfo = new SpecInfo()
    specInfo.fullname = 'The feature name'

    when:
    reportListener.beforeSpec specInfo

    then:
    stringWriter.toString() == 'Feature: The feature name\n'

    and:
    reportListener.indent == ReportListener.INDENT_STEP
  }

  def "beforeFeature prints the name of the gherkin-scenario, is indented and increases the indent level"() {
    given:
    def indent = reportListener.increaseIndent()
    def featureInfo = new FeatureInfo()
    featureInfo.name = 'This is a gherkin-scenario'

    when:
    reportListener.beforeFeature featureInfo

    then:
    stringWriter.toString() == '\n  Scenario: This is a gherkin-scenario\n'

    and:
    reportListener.indent == indent + ReportListener.INDENT_STEP
  }

  def "afterFeature prints the steps of the executed gherkin-scenario and colors the output"() {
    given: 'there is a initialized ReportListener'
    def indent = reportListener.increaseIndent()
    reportListener.color = true // force color output

    and: 'a featureInfo describing a failed feature with 3 blocks'
    def featureInfo = new FeatureInfo()
    featureInfo.successful = false
    def given = mkBlock(SETUP, ['I have this'])
    def when = mkBlock(WHEN, ['I do this', 'that'])
    def then = mkBlock(THEN, ['I want that'])
    [given, when, then].collect { featureInfo.addBlock it }

    and: 'the result is expected to be printed in red color'
    def expectedResult =
    '  \033[31mGiven I have this\n' +
        '  \033[31mWhen I do this   \n' +
        '  \033[31mWhen that        \n' +
        '  \033[31mThen I want that \n' +
        '\033[39m'

    when:
    reportListener.afterFeature featureInfo

    then:
    stringWriter.toString() == expectedResult

    and:
    reportListener.indent == indent - ReportListener.INDENT_STEP
  }

  def 'file origin should be displayed as comment if present in BlockInfo'() {
    given:
    def featureInfo = makeFeatureInfo()

    and:
    def expectedResult = '''\
      When when # ./features/step_definitions/foo.groovy:12
      Then then # ./features/step_definitions/foo.groovy:4
    '''.stripIndent()

    when:
    reportListener.afterFeature(featureInfo)

    then:
    stringWriter.toString() == expectedResult
  }

  List<IterationInfo> addTable(FeatureInfo featureInfo, List table) {
    def iterations = []

    table[0].each { featureInfo.addParameterName(it as String) }
    table[1..-1].each { List row ->
      def provider = new DataProviderInfo()
      provider.dataVariables = row
      featureInfo.addDataProvider(provider)
      iterations << new IterationInfo(row as Object[], 0)
    }

    def methodInfo = new MethodInfo()
    methodInfo.setKind(MethodKind.DATA_PROCESSOR)
    featureInfo.dataProcessorMethod = methodInfo

    iterations
  }

  def 'all steps should be printed before the first run of a scenario-outline'() {
    given:
    def featureInfo = makeFeatureInfo()
    List<IterationInfo> iterations = addTable(featureInfo, [['a', 'b', 'c'], [1, 2, 3], [22, 1, 23]])

    and:
    def expectedResult = '''
      Scenario Outline: scenario
        When when # ./features/step_definitions/foo.groovy:12
        Then then # ./features/step_definitions/foo.groovy:4
      Examples:
        | a  | b | c  |
'''.stripIndent()

    def tableRows = '''\
  | 1  | 2 | 3  |
  | 22 | 1 | 23 |
'''

    when:
    reportListener.beforeFeature(featureInfo)

    then:
    stringWriter.toString() == expectedResult

    when:
    reportListener.afterIteration(iterations[0])
    reportListener.afterIteration(iterations[1])

    then:
    stringWriter.toString() == expectedResult + tableRows
  }

  def 'pending blocks should be displayed similar to file origins of assigned blocks'() {
    given:
    def featureInfo = makePendingFeatureInfo()

    when:
    reportListener.featureSkipped(featureInfo)

    then:
    stringWriter.toString() == '''
Scenario: scenario
  When when # pending
  Then then # ./features/step_definitions/foo.groovy:4
'''.stripIndent()
  }

  def 'scenario outlines with pending blocks should be displayed with the whole table'() {
    given:
    def featureInfo = makePendingFeatureInfo()
    addTable(featureInfo, [['a', 'b'], [1, 2], [3, 4]])

    when:
    reportListener.featureSkipped(featureInfo)

    then:
    stringWriter.toString() == '''
  Scenario Outline: scenario
    When when # pending
    Then then # ./features/step_definitions/foo.groovy:4
  Examples:
    | a | b |
    | 1 | 2 |
    | 3 | 4 |
'''.stripIndent()
  }
}

