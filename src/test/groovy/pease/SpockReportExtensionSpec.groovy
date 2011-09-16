package pease

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.SpecInfo
import pease.spock.Report
import pease.spock.ReportExtension
import spock.lang.Specification
import static org.mockito.Mockito.mock

class SpockReportExtensionSpec extends Specification {
  def reportExtension = new ReportExtension()

  def "extension exists"() {
    expect:
    reportExtension instanceof AbstractAnnotationDrivenExtension
  }

  def "extension adds a new listener to the SpecInfo"() {
    given:
    def annotation = mock(Report)

    and:
    def specInfo = new SpecInfo()
    specInfo.name = 'My test spec'
    specInfo.filename = 'foo.groovy'

    expect:
    specInfo.listeners.size() == 0

    when:
    reportExtension.visitSpecAnnotation(annotation, specInfo)

    then:
    specInfo.listeners.size() == 1
  }
}