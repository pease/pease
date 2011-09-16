package pease

@Singleton
class FileLoader {
  private FileLoader() {}

  List<String> findFilesInDirectory(String directory, String regexPattern) {
    def dir = new File(directory)

    if (dir.isDirectory()) {
      def filter = { File d, String name -> name ==~ regexPattern } as FilenameFilter

      dir.list(filter).collect { directory + '/' + it }
    } else {
      []
    }
  }

  List<String> findFeatures(dir) {
    findFilesInDirectory(dir, '^.*\\.feature$')
  }

  List<String> findSteps(dir) {
    findFilesInDirectory(dir, '^.*\\.groovy$')
  }
}
