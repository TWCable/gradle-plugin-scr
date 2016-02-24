/*
 * Copyright 2014-2015 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twcable.gradle.scr

import nebula.test.PluginProjectSpec
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.logging.LogLevel
import spock.lang.Subject

@Subject(ScrPlugin)
@SuppressWarnings("GroovyPointlessBoolean")
class ScrPluginSpec extends PluginProjectSpec {

    def setup() {
        project.logging.level = LogLevel.DEBUG
    }


    def "dependsOn"() {
        project.apply plugin: pluginName

        project.apply plugin: 'java'
        def jarTask = project.tasks.getByName('jar')


        def jarDependencies = jarTask.taskDependencies.getDependencies(jarTask)

        expect:
        jarDependencies.contains(project.tasks.getByName('processScrAnnotations'))
    }


    def "sets input and output"() {
        project.apply plugin: pluginName
        project.apply plugin: 'java'

        ((DefaultProject)project).evaluate()

        def task = project.tasks.getByName('processScrAnnotations')

        expect:
        task.inputs.hasInputs == true
        task.outputs.hasOutput == true
    }


    def "does not set input and output when there are no source sets"() {
        project.apply plugin: pluginName

        ((DefaultProject)project).evaluate()

        def task = project.tasks.getByName('processScrAnnotations')

        expect:
        task.inputs.hasInputs == false
        task.outputs.hasOutput == false
    }


    @Override
    String getPluginName() {
        return "com.twcable.scr"
    }

}
