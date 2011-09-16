package pease

import org.junit.runner.Description
import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.Sputnik
import pease.gherkin.FeatureLoader
import pease.groovy.StepLoader

class ParentRunner extends org.junit.runners.ParentRunner<Sputnik> {
  public ParentRunner(Class clazz) {
    super(clazz)
  }

  String featureDirectory = './features'
  String stepsDirectory = "$featureDirectory/step_definitions"

  @Override
  protected List<Sputnik> getChildren() {
    def features = FeatureLoader.instance.loadFeatures(featureDirectory)
    def steps = StepLoader.instance.loadSteps(stepsDirectory)
    def configuration = new Configuration()

    features.collect { it.compile(steps, configuration) }.collect { new Sputnik(it.fromJust()) }
  }

  @Override
  protected Description describeChild(Sputnik child) {
    child.description
  }

  @Override
  protected void runChild(Sputnik child, RunNotifier notifier) {
    child.run(notifier)
  }
}