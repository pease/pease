package pease.support

import org.junit.runner.notification.RunNotifier
import org.spockframework.builder.DelegatingScript
import org.spockframework.runtime.JUnitSupervisor
import org.spockframework.runtime.ParameterizedSpecRunner
import org.spockframework.runtime.RunContext
import org.spockframework.runtime.model.SpecInfo
import org.spockframework.util.Nullable

class SpyRunContext extends RunContext {
  def parameterizedSpecRunner

  private SpyRunContext(@Nullable DelegatingScript configurationScript, List<Class<?>> extensionClasses) {
    super(configurationScript, extensionClasses)
  }

  @Override
  ParameterizedSpecRunner createSpecRunner(SpecInfo spec, RunNotifier notifier) {
    parameterizedSpecRunner = new SpyParameterizedSpecRunner(spec,
            new JUnitSupervisor(spec, notifier, createStackTraceFilter(spec), diffedObjectRenderer))
  }

  Object getCurrentInstance() {
    parameterizedSpecRunner.currentInstance
  }


}
