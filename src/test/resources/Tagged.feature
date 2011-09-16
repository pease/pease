@Tag1 @Tag2
Feature: Tag features to select a subset of features
  As a productive developer
  I want to tag my features
  In order to only run those features, that I am working on

  @ScenarioTag
  Scenario: A scenario tagged scenario

  @Tag3
  Scenario: Inheritance of tags
    * This scenario gets called when Tag1, Tag2 or Tag3 are selected


  @OutlineTag
  Scenario Outline: An outline that has a tag

  @Tag4
  Scenario: Inheritance of tags from Scenario Outline and Feature
    * This scenario gets called when Tag1, Tag3, Tag3, OutlineTag or Tag4 are selected