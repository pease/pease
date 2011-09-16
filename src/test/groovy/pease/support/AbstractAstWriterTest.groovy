package pease.support

import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase
import org.objectweb.asm.Opcodes
import pease.groovy.AstWriter
import spock.lang.Specification

// AbstractClassWithoutAbstractMethod: This class should not be instantiated, even if it is not by the pure definition an abstract class
@SuppressWarnings('AbstractClassWithoutAbstractMethod')
abstract class AbstractAstWriterTest extends Specification implements Opcodes {
    StringBuffer sb = new StringBuffer()
    def visitor = new AstWriter(sb)

    protected buildAstFrom(String string) {
        new AstBuilder().buildFromString(CompilePhase.CONVERSION, string)
    }
}