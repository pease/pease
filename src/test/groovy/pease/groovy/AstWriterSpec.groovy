package pease.groovy

import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.objectweb.asm.Opcodes
import pease.support.AbstractAstWriterTest
import spock.lang.Ignore
import spock.lang.Specification
import org.codehaus.groovy.ast.*

class AstWriterSpec extends AbstractAstWriterTest {
  def "check modifiers string representation"() {
    when:
    def s = AstWriter.modifiersToString(modifiers as Integer)

    then:
    s == string

    where:
    modifiers << [ACC_PUBLIC | ACC_STATIC, ACC_PRIVATE]
    string << ['public static', 'private']
  }

  def "class to string"() {
    given:
    def nodes = new AstBuilder().buildFromSpec {
      classNode('MySpec', ACC_PUBLIC) {
        classNode(Specification)
        interfaces {
          classNode GroovyObject
        }
        mixins {}
        genericsTypes {}
      }
    }
    ClassNode classNode = (ClassNode) nodes[0]

    when:
    new AstWriter(sb).visitClass(classNode)

    then:
    sb.toString() == '''\
    public class MySpec extends spock.lang.Specification {
    }
    '''.stripIndent()
  }

  def "method to string"() {
    given:
    def parameter = [new Parameter(ClassHelper.make(String), 'bar'), new Parameter(ClassHelper.make(Object), 'fortyTwo')] as Parameter[]

    and:
    List<ASTNode> code = new AstBuilder().buildFromCode {
      return 42
    }

    and:
    MethodNode methodNode = new MethodNode('foo', ACC_PUBLIC, ClassHelper.OBJECT_TYPE, parameter, ClassNode.EMPTY_ARRAY, (Statement) code[0])

    when:
    new AstWriter(sb).visitMethod methodNode

    then:
    sb.toString() == '''\
        public java.lang.Object foo(java.lang.String bar, java.lang.Object fortyTwo) {
          return 42
        }
    '''.stripIndent()
  }

  def "if else to string"() {
    given:
    def nodes = new AstBuilder().buildFromCode {
      if (1 == 2) {
        5
      } else {
        42
      }

      if ("single if" == 'should work') {
        23
      }
      println 'works'
    }.find { it.class == BlockStatement }.statements

    and:
    def visitor = new AstWriter(sb)

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == /\
        if (1 == 2) {
          5
        } else {
          42
        }
        if ('single if' == 'should work') {
          23
        }
        return this.println('works')
    /.stripIndent()
  }

  def "closure expression"() {
    given:
    def nodes = new AstBuilder().buildFromCode {
      [1, 2, 3].collect {
        it * 2
      }
    }.find { it.class == BlockStatement }.statements

    and:
    def expectedResult = '''\
        return [1, 2, 3].collect({ ->
          return it * 2
        })
    '''.stripIndent()

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == expectedResult
  }

  def "GString"() {
    given:
    def nodes = new AstBuilder().buildFromCode {
      def a = 5
      "I am a GString and contain the variable $a"
    }

    and:
    def expectedResult = '''\
        {
          java.lang.Object a = 5
          return "I am a GString and contain the variable $a"
        }
    '''.stripIndent()

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == expectedResult
  }

  def "normal strings"() {
    given:
    def nodes = buildAstFrom 'def a = \'I am a "string" with another string\''

    and:
    def expectedResult = '''\
        {
          java.lang.Object a = 'I am a "string" with another string'
        }
    '''.stripIndent()

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == expectedResult
  }

  def "multiple assignment"() {
    given:
    def nodes = new AstBuilder().buildFromCode {
      def (a, b) = [1, 2]
      assert a == 1
      assert b == 2, 'b must be 2'
      return b
    }

    and:
    def expectedResult = '''\
        {
          java.lang.Object (a, b) = [1, 2]
          assert a == 1
          assert b == 2, 'b must be 2'
          return b
        }
    '''.stripIndent()

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == expectedResult
  }

  def "switch statement"() {
    given:
    def nodes = new AstBuilder().buildFromCode {
      def a = 'my string'
      switch (a) {
        case ~/^my.*/:
          println 'first match'
          break

        case 'my string':
          println 'long match'
          break

        default:
          println 'no match'
      }
    }

    and:
    def expectedResult = '''\
        {
          java.lang.Object a = 'my string'
          switch (a) {
            case ~'^my.*':
              return this.println('first match')
              break
            case 'my string':
              return this.println('long match')
              break
            default:
              return this.println('no match')
          }
          return null
        }
    '''.stripIndent()

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == expectedResult
  }

  def "for loop"() {
    given:
    def nodes = new AstBuilder().buildFromCode {
      def accu = 0
      for (int i = 0; i < 5; ++i) {
        accu += i
      }
      assert accu == 15
      return accu
    }

    and:
    String expectedResult = '''\
        {
          java.lang.Object accu = 0
          for (java.lang.Integer i = 0; i < 5; ++i) {
            accu += i
          }
          assert accu == 15
          return accu
        }
    '''.stripIndent()

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == expectedResult
  }

  def "labels"() {
    given:
    def nodes = buildAstFrom '''
        L1: println "Hello world"
        L2: def a = 1 + 0
        L3: def b = a == 1
        String withoutLabel = "works, too"
    '''

    and:
    def expectedResult = '''\
        {
          L1: this.println('Hello world')
          L2: java.lang.Object a = 1 + 0
          L3: java.lang.Object b = a == 1
          String withoutLabel = 'works, too'
        }
    '''.stripIndent()

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == expectedResult
  }

  def "closure assignment and call"() {
    given:
    def nodes = buildAstFrom '''
      def a = {
         'hello'
      }()
      a = {
         'booh'
      }()
    '''

    and:
    def expectedResult = '''\
      {
        java.lang.Object a = { ->
          'hello'
        }.call()
        a = { ->
          'booh'
        }.call()
      }
    '''.stripIndent()

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == expectedResult
  }

  def "property access"() {
    given:
    def nodes = buildAstFrom 'def a = [foo: 2]; a.foo = 5'

    and:
    def expectedResult = '''\
      {
        java.lang.Object a = ['foo': 2]
        a.'foo' = 5
      }
    '''.stripIndent()

    when:
    nodes.each { it.visit visitor }

    then:
    sb.toString() == expectedResult
  }

  def "static method call"() {
    given:
    def ast = new MethodCallExpression(
        new ClassExpression(ClassHelper.STRING_TYPE),
        new ConstantExpression("valueOf"),
        ArgumentListExpression.EMPTY_ARGUMENTS)

    when:
    ast.visit visitor

    then:
    sb.toString() == '''\
      java.lang.String.valueOf()
    '''.stripIndent()
  }

  def 'method annotations should be displayed'() {
    given:
    def method = new MethodNode('foo', Opcodes.ACC_PUBLIC, ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, new BlockStatement())

    and:
    def ignore = new AnnotationNode(ClassHelper.make(Ignore))
    ignore.setMember('value', new ConstantExpression('foo'))
    method.addAnnotation(ignore)

    and:
    def expected = '''\
      @spock.lang.Ignore(value='foo')
      public void foo() {
      }
    '''.stripIndent()

    when:
    visitor.visitMethod(method)

    then:
    sb.toString() == expected
  }
}
