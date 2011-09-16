package pease.groovy.model

import groovy.transform.TupleConstructor
import org.codehaus.groovy.ast.ClassNode

@TupleConstructor
class WorldNode {
  final ClassNode worldClass
  String filename
}
