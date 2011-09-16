Feature: Table support
  As a test writer
  I want to specify tabular data
  In order to structure data for better readability

  Scenario: Result table
    Given I have a scenario that does something
    When I specify my steps using a parameter <x>
    Then there may be a result of <y>
    Examples:
      | x               | y               |
      | my first input  | my first result |
      | my second input | another result  |

  Scenario: Inline tables
    Given I have a scenario were a specify some tabular data
    And the table contains a list of data that is required to fulfill the test, like:
      | name            | age |
      | Albert Einstein | 133 |
      | Donald Knuth    | 74  |
      | James Gossling  | 57  |
    When I start a search for legendary scientists, that are still alive
    Then I get the following table
      | name           |
      | Donald Knuth   |
      | James Gossling |

