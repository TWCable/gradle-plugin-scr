# Purpose #

This makes it easier to work for with
[the Felix SCR annotations](https://felix.apache.org/documentation/subprojects/apache-felix-maven-scr-plugin/scr-annotations.html)
for Gradle and Apache Felix (including Adobe CQ/AEM).

[ ![Download](https://api.bintray.com/packages/twcable/aem/gradle-plugin-scr/images/download.svg) ](https://bintray.com/twcable/aem/gradle-plugin-scr/_latestVersion)

# Usage #

`apply plugin: 'com.twcable.scr'`

Applying the plugin implicitly applies the `osgi` plugin
  and adds a dependency from the `jar` task upon `processScrAnnotations`.

## Task ##

* `processScrAnnotations` - Processes the x-doclet style and @SCR annotations to create
  the appropriate OSGi metadata for OSGi Declarative Services.

# Installation #

```
buildscript {
    repositories {
        maven {
            url "http://dl.bintray.com/twcable/aem"
        }
    }

    dependencies {
        classpath "com.twcable.gradle:gradle-plugin-scr:<version>"
    }
}
```

Built against **Gradle 2.8**

# API #

https://twcable.github.io/gradle-plugin-scr/docs/groovydoc/

# LICENSE

Copyright 2015 Time Warner Cable, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
the specific language governing permissions and limitations under the License.
