package pease.spock

import org.spockframework.runtime.AbstractRunListener
import org.spockframework.runtime.model.*

class ReportListener extends AbstractRunListener {
  final static int INDENT_STEP = 2
  final static RED_COLOR = 1
  final static GREEN_COLOR = 2
  PrintStream stream
  int indent = 0
  boolean color = false
  List<Integer> columnMaxLength = []

  def increaseIndent() {
    indent += INDENT_STEP
  }

  def decreaseIndent() {
    indent -= INDENT_STEP
  }

  void indentBlock(Closure closure) {
    increaseIndent()
    closure.call()
    decreaseIndent()
  }

  void printWithIndent(Closure closure) {
    printIndent()
    closure.call()
  }

  def printIndent() {
    indent.times {
      stream.print ' '
    }
  }

  public ReportListener() {
    this(System.out)
  }

  public ReportListener(PrintStream stream) {
    this.stream = stream
  }

  @Override
  void beforeSpec(SpecInfo spec) {
    stream.println "Feature: $spec.fullname"
    increaseIndent()
  }

  void printFeatureStart(FeatureInfo feature) {
    stream.print '\n'

    printWithIndent {
      stream.print feature.parameterized ? 'Scenario Outline' : 'Scenario'
      stream.println ": $feature.name"
    }
  }

  List calculateMaxColumnLength(FeatureInfo feature) {
    def columnMaxLength = []

    feature.parameterNames.eachWithIndex { String name, int index ->
      columnMaxLength[index] = [columnMaxLength[index], name.length()].max()
    }

    (0..(feature.parameterNames.size() - 1)).each { index ->
      columnMaxLength[index] = feature.dataProviders.collect { it.dataVariables[index] }?.collect { it.toString().length() }?.max()
    }

    columnMaxLength
  }

  @Override
  void beforeFeature(FeatureInfo feature) {
    printFeatureStart(feature)

    if (feature.parameterized) {
      indentBlock {
        printBlocks(feature, false)
      }

      columnMaxLength = calculateMaxColumnLength(feature)

      printWithIndent {
        stream.println 'Examples:'

        indentBlock {
          printTableRow(feature.parameterNames)
        }
      }
    }

    increaseIndent()
  }

  @Override
  void featureSkipped(FeatureInfo feature) {
    printFeatureStart(feature)

    indentBlock {
      printBlocks(feature, false)
    }

    if (feature.parameterized) {
      columnMaxLength = calculateMaxColumnLength(feature)

      printWithIndent {
        stream.println 'Examples:'

        indentBlock {
          printTableRow(feature.parameterNames)

          feature.dataProviders.each {
            printTableRow(it.dataVariables)
          }
        }
      }
    }
  }


  void printBlocks(FeatureInfo feature, boolean color = true) {
    List<BlockInfo> blocks = feature.blocks.grep { it.kind != BlockKind.WHERE }

    def blockSteps = blocks.collect { BlockInfo block -> block.texts.collect { "${format block.kind} $it" }  }
    def blockFiles = blocks.collect { BlockInfo block -> block.fileOrigins }
    int longestLine = (int) blockSteps.flatten().collect { it.length() }.max()

    blockSteps.eachWithIndex { block, blockIndex ->
      def files = blockFiles[blockIndex]
      block.eachWithIndex { text, index ->
        printWithIndent {
          feature.successful ? useColor('green') : useColor('red')

          stream.print(text.padRight(longestLine))

          if (index < files.size()) {
            stream.print(" # ${files[index]}")
          }
          stream.println()
        }
      }
    }
  }

  @Override
  void afterFeature(FeatureInfo feature) {
    if (!feature.parameterized) {
      printBlocks(feature, true)
    } else {
      // decrease indent from example table rows
      decreaseIndent()
    }

    resetColor()
    decreaseIndent()
  }

  void printTableRow(columns) {
    int index = 0
    printWithIndent {
      stream.println columns.inject('|') { rest, item ->
        rest + " ${(item as String).padRight(columnMaxLength[index++])} |"
      }
    }
  }

  @Override
  void afterIteration(IterationInfo iteration) {
    printTableRow(iteration.dataValues)
  }

  def useColor(String colorName) {
    if (color) {
      def code = null

      switch (colorName) {
        case 'green': code = GREEN_COLOR
          break

        case 'red': code = RED_COLOR
          break
      }

      if (code) {
        stream.print "\033[3${code}m"
      }
    }
  }

  def resetColor() {
    if (color) {
      stream.print '\033[39m'
    }
  }

  def format(BlockKind blockKind) {
    switch (blockKind) {
      case BlockKind.SETUP:
        'Given'
        break

      case BlockKind.EXPECT:
        'Expect'
        break

      case BlockKind.WHEN:
        'When'
        break

      case BlockKind.THEN:
        'Then'
        break

      case BlockKind.THEN:
        'Cleanup'
        break

      case BlockKind.THEN:
        'Where'
        break

      default:
        throw new IllegalArgumentException("Unhandeled enum member $blockKind")
    }
  }
}
