package pease.support

class SpecFileHelper {
  def getResourceFile(name) {
    this.class.getResource(name)?.file
  }

  def loadFileContents(String filename) {
    this.class.getResourceAsStream(filename)?.text
  }
}
