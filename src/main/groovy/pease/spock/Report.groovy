package pease.spock

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import org.spockframework.runtime.extension.ExtensionAnnotation

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE])
@ExtensionAnnotation(ReportExtension)
@interface Report {
  // TODO is this the right way to toggle color output? It should be a runtime configuration, rather than a compiled property.
  boolean useColor() default false
}
