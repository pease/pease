When(~/the numbers (\d) and (\d) are added/) { a, b ->
  def result = (a as int) + (b as int)
}

Then(~/the number (\d) should result/) { res ->
  result == (res as int)
}
