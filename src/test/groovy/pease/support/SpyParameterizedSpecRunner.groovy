package pease.support

import org.spockframework.runtime.JUnitSupervisor
import org.spockframework.runtime.ParameterizedSpecRunner
import org.spockframework.runtime.model.SpecInfo

// this class has access to current instance
class SpyParameterizedSpecRunner extends ParameterizedSpecRunner {
  SpyParameterizedSpecRunner(SpecInfo specInfo, JUnitSupervisor jUnitSupervisor) {
    super(specInfo, jUnitSupervisor)
  }

  Object getCurrentInstance() {
    currentInstance
  }

}
