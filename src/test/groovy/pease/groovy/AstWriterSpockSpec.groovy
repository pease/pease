package pease.groovy

import org.codehaus.groovy.ast.ClassNode
import pease.support.AbstractAstWriterTest

/**
 * Test typical Spock specifications.
 */
class AstWriterSpockSpec extends AbstractAstWriterTest {
    def "simple spec with"() {
        given:
        def nodes = buildAstFrom """\
            class MySpec extends spock.lang.Specification {
              def "my test"() {
                given: "I have this"
                def a = 2

                when: "I do this"
                a *= 2

                then: "I want that"
                a == 4
              }
            }
        """.stripIndent()

        and:
        def expectedResult = '''\
            public class MySpec extends spock.lang.Specification {
              public java.lang.Object "my test"() {
                given: 'I have this'
                java.lang.Object a = 2
                when: 'I do this'
                a *= 2
                then: 'I want that'
                a == 4
              }
            }
        '''.stripIndent()

        when:
        nodes.findAll {it.class == ClassNode }.each { ClassNode node -> visitor.visitClass(node) }

        then:
        sb.toString() == expectedResult
    }

    def "data tables"() {
        given:
        def nodes = buildAstFrom """
            import spock.lang.Specification

            class MySpec extends Specification {
                def "data table test"() {
                   expect:
                   a * 2 == b

                   where: ""
                   a  |  b
                   4  |  8
                   8  | 16
                   32 | 64
                }
            }
        """

        and:
        def expectedResult = """\
            public class MySpec extends Specification {
              public java.lang.Object "data table test"() {
                expect: a * 2 == b
                where: ''
                a | b
                4 | 8
                8 | 16
                32 | 64
              }
            }
        """.stripIndent()

        when:
        nodes.findAll { it.class == ClassNode }.each { ClassNode node -> visitor.visitClass(node) }

        then:
        sb.toString() == expectedResult

    }

}