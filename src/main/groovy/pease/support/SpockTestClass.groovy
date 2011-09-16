package pease.support

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
  * Used to annotate the name of the spock test responsible for this class (for documentation).
  */
@Target([ElementType.TYPE])
@Retention(RetentionPolicy.RUNTIME)
@interface SpockTestClass {
  // Class values can not be used, because gradle separates the test and main sources
  String value()
}
