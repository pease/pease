package pease.spock

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.SpecInfo
import pease.support.SpockTestClass

@SpockTestClass('SpockReportExtensionSpec')
class ReportExtension extends AbstractAnnotationDrivenExtension<Report> {
  void visitSpecAnnotation(Report annotation, SpecInfo spec) {
    def listener = new ReportListener()

    if (annotation.useColor()) {
      listener.color = true
    }

    spec.addListener(listener)
  }
}
