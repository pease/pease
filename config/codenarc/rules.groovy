ruleset {
  description 'custom groovy rules'

  ruleset('rulesets/basic.xml') {
    'InvertedIfElse' priority: 3
  }

  ruleset('rulesets/braces.xml')
  ruleset('rulesets/concurrency.xml')
  ruleset('rulesets/design.xml')
  ruleset('rulesets/dry.xml')
  ruleset('rulesets/exceptions.xml')
  ruleset('rulesets/generic.xml') {
    'StatelessClass' enabled: false
  }
  ruleset('rulesets/grails.xml')
  ruleset('rulesets/imports.xml')
  ruleset('rulesets/logging.xml') {
   'Println' priority: 1
   'PrintStackTrace' priority: 1
  }
  ruleset('rulesets/junit.xml')
  ruleset('rulesets/naming.xml') {
    'ClassName' regex: /[A-Z][\w\$]*/

    // this rules caused many Spock test related false positives
    'MethodName' enabled: false 
  }
  ruleset('rulesets/size.xml')
  ruleset('rulesets/unnecessary.xml')
  ruleset('rulesets/unused.xml')
}
