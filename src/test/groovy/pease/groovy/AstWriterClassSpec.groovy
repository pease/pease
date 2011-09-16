package pease.groovy

import org.codehaus.groovy.ast.ClassNode
import pease.support.AbstractAstWriterTest

/**
 * Test the "Class" parts of AstWriter.
 */
class AstWriterClassSpec extends AbstractAstWriterTest {
  def "test the inner class processing"() {
    given:
    def nodes = buildAstFrom '''
        final class MyClass extends AbstractList {
          private class MyInnerClass {
            def myInnerMethod() {
            }
          }

          private MyClass() {
            super()
            MyInnerClass myInnerClass = new MyInnerClass()
            myInnerClass.myInnerMethod()
          }

          public static MyClass mk() {
            MyClass foo = new MyClass()

            return foo
          }

          Object get(int index) { null }
          int size() { 0 }
        }
     '''.stripIndent()

    and:
    String expectedResult = '''\
        public final class MyClass extends AbstractList {
          private class MyClass$MyInnerClass {
            public java.lang.Object myInnerMethod() {
            }
          }
          private MyClass() {
            super()
            MyInnerClass myInnerClass = new MyInnerClass()
            myInnerClass.myInnerMethod()
          }
          public static MyClass mk() {
            MyClass foo = new MyClass()
            return foo
          }
          public Object get(int index) {
            null
          }
          public int size() {
            0
          }
        }
    '''.stripIndent()

    when:
    nodes.grep {
      it.class.isAssignableFrom(ClassNode)
    }.each { ClassNode node ->
      visitor.visitClass(node)
    }

    then:
    sb.toString() == expectedResult
  }

  def "class properties"() {
    setup:
    def nodes = buildAstFrom '''
        class MyClass {
          def prop1
          def prop2
        }
    '''

    and:
    def expectedResult = '''\
      public class MyClass {
        java.lang.Object prop1
        java.lang.Object prop2
      }
    '''.stripIndent()

    when:
    nodes.findAll {it.class == ClassNode}.each { ClassNode node -> visitor.visitClass(node) }

    then:
    sb.toString() == expectedResult
  }

  def "annotated class"() {
    setup:
    def nodes = buildAstFrom '''
        @TupleConstructor(includeFields=true, includeProperties=true)
        @Foobar
        class Person {
          String name
        }
    '''

    and:
    def expectedResult = '''\
        @TupleConstructor(includeFields=true, includeProperties=true)
        @Foobar
        public class Person {
          String name
        }
    '''.stripIndent()

    when:
    nodes.findAll { it.class == ClassNode }.each { ClassNode node -> visitor.visitClass(node) }

    then:
    sb.toString() == expectedResult
  }

  def "annotated fields"() {
    setup:
    def nodes = buildAstFrom '''
        class Person {
           @org.spockframework.util.Nullable
           @Foobar
           String name
        }
    '''

    and:
    def expectedResult = '''\
        public class Person {
          @org.spockframework.util.Nullable
          @Foobar
          String name
        }
    '''.stripIndent()

    when:
    nodes.findAll { it.class == ClassNode }.each { ClassNode node -> visitor.visitClass(node) }

    then:
    sb.toString() == expectedResult
  }
}