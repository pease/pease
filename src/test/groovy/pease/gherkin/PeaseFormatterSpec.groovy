package pease.gherkin

import gherkin.formatter.model.Feature
import spock.lang.Specification

class PeaseFormatterSpec extends Specification {
    PeaseFormatter peaseFormatter = new PeaseFormatter()

    def mkFeature(name) {
        new Feature(null, null, 'Feature', name, '', 0)
    }

    def "a feature should be returned as an object"() {
        when:
        peaseFormatter.feature(mkFeature('foo'))

        then:
        peaseFormatter.feature != null
    }
}
