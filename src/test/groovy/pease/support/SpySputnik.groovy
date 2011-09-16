package pease.support

import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.Sputnik

// Provides access to the currentInstance field, to check some state within the test class instance.
@SuppressWarnings('GroovyAccessibility')
class SpySputnik extends Sputnik {
  def specRunner
  def myClass

  public SpySputnik(Class<?> clazz) {
    super(clazz)
    myClass = clazz
  }

  @Override
  void run(RunNotifier notifier) {
    super.runExtensionsIfNecessary()
    super.aggregateDescriptionIfNecessary()
    specRunner = SpyRunContext.get().createSpecRunner(super.spec, notifier)
    specRunner.run()
  }

  Object getCurrentInstance() {
    specRunner.currentInstance
  }
}
