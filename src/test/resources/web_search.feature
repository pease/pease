@geb
Feature: Web search
  In order to test the Geb integration
  As a Pease developer
  I want to do a search on some search engines

  Scenario: Search at Google
    Given Im on the Google main page
    When I enter "wikipedia" into the search box
    And press the "search button"
    Then I should see "Wikipedia" as first search result
