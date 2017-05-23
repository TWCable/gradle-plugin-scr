/*
 * Copyright 2014-2017 Time Warner Cable, Inc.
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

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar

@CompileStatic
class ScrPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.apply(plugin: 'osgi')

        addScrTask(project)
    }


    static void addScrTask(Project project) {
        final processScrAnnotations = project.tasks.create('processScrAnnotations', ProcessScrAnnotationsTask)
        processScrAnnotations.with {
            group = 'Build'
            description = 'Processes the Felix SCR service annotations'
            dependsOn 'classes'
            project.afterEvaluate {
                def mainSourceSet = mainSourceSet()
                if (mainSourceSet != null) {
                    def classesDir = mainSourceSet.output.classesDir
                    inputs.dir classesDir
                    outputs.dir new File(classesDir, 'OSGI-INF')
                }
            }
        }
        project.tasks.withType(Jar) { Task task ->
            task.dependsOn processScrAnnotations
        }
    }

}
