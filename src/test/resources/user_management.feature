@run
Feature: User management
  As a system owner
  I want to keep record of the users
  In order to bill them

  Scenario: Filter users
    Given I have following users
      | user    | email                |
      | gcantor | george@cantor.de     |
      | leibniz | gottfried@leibniz.de |
      | hcurry  | haskell@curry.com    |

    When I filter those from Germany

    Then I should have the following users
      | user    | email                |
      | gcantor | george@cantor.de     |
      | leibniz | gottfried@leibniz.de |