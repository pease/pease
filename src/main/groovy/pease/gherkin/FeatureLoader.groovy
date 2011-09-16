package pease.gherkin

import gherkin.formatter.Formatter
import gherkin.lexer.I18nLexer
import gherkin.lexer.Lexer
import gherkin.lexer.LexingError
import gherkin.lexer.Listener
import gherkin.parser.FormatterListener
import pease.FileLoader
import pease.gherkin.model.FeatureNode
import pease.support.Maybe
import pease.support.Maybe.Just
import pease.support.Maybe.Nothing
import pease.support.SpockTestClass

@SpockTestClass('FeatureLoaderSpec')
@Singleton
class FeatureLoader {
  private FeatureLoader() {}

  Maybe<FeatureNode> loadFromString(String feature) throws LexingError {
    Formatter peaseFormatter = new PeaseFormatter()
    Listener listener = new FormatterListener(peaseFormatter)
    Lexer lexer = new I18nLexer(listener)

    lexer.scan(feature)

    peaseFormatter.feature ? new Just(peaseFormatter.feature) : Nothing.instance
  }

  Maybe<FeatureNode> loadFromFile(String featureFilename) throws LexingError {
    def file = new File(featureFilename)

    loadFromString(file.text) >>> { feature -> feature.fileName = featureFilename; new Just(feature) }
  }

  List<FeatureNode> loadFeatures(String directory) {
    def features = []

    for (featureFile in FileLoader.instance.findFeatures(directory)) {
      // FP experts will disagree, but in this case it's nice to abuse the side effect property of closures ;-)
      loadFromFile(featureFile) >>> { features << it; Nothing.instance }
    }

    features
  }
}

