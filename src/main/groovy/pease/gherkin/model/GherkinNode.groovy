package pease.gherkin.model

import pease.gherkin.GherkinVisitor

abstract class GherkinNode {
   abstract void visit(GherkinVisitor visitor)
}
