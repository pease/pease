package pease.groovy.model

import groovy.transform.TupleConstructor

// the purpose of this class is to mark pending blocks
@TupleConstructor(includeSuperProperties = true)
class PendingSpockBlock extends SpockBlock {
}
