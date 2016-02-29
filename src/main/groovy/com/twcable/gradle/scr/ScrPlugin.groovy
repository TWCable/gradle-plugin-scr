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

import groovy.transform.CompileStatic
import groovy.util.slurpersupport.Attributes
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild
import org.apache.felix.scrplugin.ant.SCRDescriptorTask
import org.apache.tools.ant.types.Path
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.osgi.OsgiManifest
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar

@CompileStatic
class ScrPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.apply(plugin: 'osgi')

        addScrTask(project)
    }


    static void addScrTask(Project project) {
        final processScrAnnotations = project.tasks.create('processScrAnnotations')
        processScrAnnotations.with {
            group = 'Build'
            description = 'Processes the Felix SCR service annotations'
            dependsOn 'classes'
            project.afterEvaluate {
                def mainSourceSet = mainSourceSet(project)
                if (mainSourceSet != null) {
                    def classesDir = mainSourceSet.output.classesDir
                    inputs.dir classesDir
                    outputs.dir new File(classesDir, 'OSGI-INF')
                }
            }

            doLast {
                processScr(project)
            }
        }
        project.tasks.withType(Jar) { Task task ->
            task.dependsOn processScrAnnotations
        }
    }


    static void processScr(Project project) {
        final mainSourceSet = mainSourceSet(project)
        final antProject = project.ant.project
        final classesDir = mainSourceSet.output.classesDir
        final runtimeClasspath = mainSourceSet.runtimeClasspath
        final runtimePath = runtimeClasspath.asPath

        project.logger.info "Running SCR for ${classesDir}"
        if (classesDir.exists()) {
            final task = new SCRDescriptorTask(srcdir: classesDir, destdir: classesDir,
                classpath: new Path(antProject, runtimePath), strictMode: false, project: antProject, scanClasses: true)
            task.execute()

            addToManifest(project, classesDir)
        }
    }


    static void addToManifest(Project project, File classesDir) throws InvalidUserDataException {
        final osgiInfDir = new File(classesDir, 'OSGI-INF')

        def files = osgiInfDir.listFiles({ File dir, String name ->
            name.endsWith(".xml")
        } as FilenameFilter) as List<File>

        if (files != null && !files.isEmpty()) {
            def relFiles = files.collect { file -> 'OSGI-INF/' + file.name }
            project.logger.info "Created ${relFiles}"
            final jar = (Jar)project.tasks.getByName('jar')
            final osgiManifest = (OsgiManifest)jar.manifest
            osgiManifest.instruction('Service-Component', relFiles.join(','))
            validateReferences(project, files)
        }
        else {
            project.logger.warn "${osgiInfDir}/*.xml was not created"
        }
    }

    /**
     * Walk through the files and make sure that any @Reference is to an interface instead of a class. If there are
     * any problems, the error messages are gathered together an an {@link InvalidUserDataException} is thrown.
     */
    static void validateReferences(Project project, List<File> files) throws InvalidUserDataException {
        def errorMessage = ""

        final classLoader = loadClassPaths(project)

        for (file in files) {
            def component = new XmlSlurper().parse(file)

            def compName = attrText(component, "name")

            component.getProperty("reference").each { NodeChild references ->
                errorMessage = generateErrorMsg(errorMessage, attrText(references, "interface"), compName, classLoader)
            }
        }

        if (errorMessage != "") {
            throw new InvalidUserDataException(errorMessage)
        }
    }


    static String attrText(GPathResult node, String attrName) {
        return ((Attributes)node.getProperty("@" + attrName)).text()
    }


    static String generateErrorMsg(String errorMessage, String interfaceName, String className,
                                   ClassLoader classLoader) {
        try {
            def clas = classLoader.loadClass(interfaceName)

            if (!clas.isInterface()) {
                errorMessage += "\n${className} has an @Reference to ${interfaceName}, but it is not an interface."
            }
        }
        catch (ClassNotFoundException ignored) {
            errorMessage += "\n${className} has an @Reference to ${interfaceName} that could not be found by the class loader."
        }
        return errorMessage
    }

    /**
     * Create a ClassLoader for the main runtimeClasspath
     */
    static ClassLoader loadClassPaths(Project project) throws InvalidUserDataException {
        def classpathURLs = mainSourceSet(project).runtimeClasspath.collect { File f -> f.toURI().toURL() }
        if (!classpathURLs) throw new InvalidUserDataException("Runtime class path empty.")
        return new URLClassLoader(classpathURLs as URL[], Thread.currentThread().contextClassLoader)
    }

    /**
     * Find the "main" SourceSet. This is added by default by the 'java' plugin and those that extend from it
     * @return null if it can't be found
     */
    static SourceSet mainSourceSet(Project project) {
        def dynamicObject = project.convention.extensionsAsDynamicObject
        if (dynamicObject.hasProperty('sourceSets')) {
            def sourceSets = dynamicObject.getProperty('sourceSets') as SourceSetContainer
            def main = sourceSets.asMap.get('main') as SourceSet

            if (main != null) return main

            project.logger.error "Could not find \"sourceSets.main\" in ${project} when applying ${this.class.name}"
            return null
        }
        project.logger.error "Could not find \"sourceSets\" in ${project} when applying ${this.class.name}"
        return null
    }

}
