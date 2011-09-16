Scenario: Defining a scenario without feature
  When I have a feature file like this
  And it contains no scenario
  And it gets loaded
  Then an error message must occur