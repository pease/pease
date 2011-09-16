package pease

enum StepKind {
  GIVEN('Given', 'given'), WHEN('When', 'when'), THEN('Then', 'then'), AND('and'), UNCLASSIFIED('Unclassified', '')

  final String text = ''
  final String label = ''

  StepKind(String text, String label) {
    this.text = text
    this.label = label
  }

  StepKind(String label) {
    this('', label)
  }

  static StepKind fromString(String s) {
    switch (s) {
      case ~/Given\s*/:
        return GIVEN
      case ~/When\s*/:
        return WHEN
      case ~/Then\s*/:
        return THEN
      default:
        return UNCLASSIFIED
    }
  }
}
