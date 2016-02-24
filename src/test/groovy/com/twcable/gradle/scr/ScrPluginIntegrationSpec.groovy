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
import nebula.test.IntegrationSpec
import org.gradle.api.logging.LogLevel
import spock.lang.Subject

@Subject(ScrPlugin)
@SuppressWarnings("GroovyPointlessBoolean")
class ScrPluginIntegrationSpec extends IntegrationSpec {

    def "simple run"() {
        logLevel = LogLevel.INFO

        writeBuildFile()

        def simpleServiceFile = writeSimpleService("com.test", projectDir)
        def simpleServletFile = writeSimpleServlet("com.test", true, projectDir)

        when:
        def result = runTasks("processScrAnnotations")

        then:
        result.success

        and: "generated for SimpleService"
        componentFile(simpleServiceFile).exists() == true
        metatypeFile(simpleServiceFile).exists() == true
        metatypePropertiesFile(simpleServiceFile).exists() == true

        and: "generated for SimpleServlet"
        componentFile(simpleServletFile).exists() == true
        metatypeFile(simpleServletFile).exists() == false
        metatypePropertiesFile(simpleServletFile).exists() == false

        cleanup:
        if (result) println result.standardOutput
        if (result) println result.standardError
    }


    def "trying to use class instead of interface for @Reference"() {
        logLevel = LogLevel.INFO

        writeBuildFile()

        writeSimpleService("testpkg2", projectDir)
        writeSimpleServlet("testpkg2", false, projectDir)

        when:
        def result = runTasks("processScrAnnotations")

        then:
        result.success == false
        Throwable exception = result.failure.cause
        "Execution failed for task ':processScrAnnotations'." == exception.message
        def cause = exception.cause
        "\ntestpkg2.SimpleServlet has an @Reference to testpkg2.SimpleService, but it is not an interface." == cause.message

        cleanup:
        if (result) println result.standardOutput
        if (result) println result.standardError
    }


    private File writeBuildFile() {
        return buildFile << '''
            apply plugin: 'java'
            apply plugin: 'com.twcable.scr'

            repositories {
                jcenter()
            }
            dependencies {
                compile "org.apache.felix:org.apache.felix.scr.annotations:1.9.10"
                compile "javax.servlet:servlet-api:2.5"
                compile "ch.qos.logback:logback-classic:1.0.4"
            }
        '''.stripIndent()
    }


    @CompileStatic
    File componentFile(File sourceFile) {
        return new File(baseDir(sourceFile), "build/classes/main/OSGI-INF/${sourceClassname(sourceFile)}.xml")
    }


    @CompileStatic
    File metatypeFile(File sourceFile) {
        return new File(baseDir(sourceFile), "build/classes/main/OSGI-INF/metatype/${sourceClassname(sourceFile)}.xml")
    }


    @CompileStatic
    File metatypePropertiesFile(File sourceFile) {
        return new File(baseDir(sourceFile), "build/classes/main/OSGI-INF/metatype/${sourceClassname(sourceFile)}.properties")
    }


    @CompileStatic
    File baseDir(File sourceFile) {
        def sourcePath = sourceFile.absolutePath
        def indexOfLeadFromSourcePath = sourcePath.lastIndexOf('src/main/java/')
        def basePath = sourcePath.substring(0, indexOfLeadFromSourcePath)
        return new File(basePath)
    }


    @CompileStatic
    String sourceClassname(File sourceFile) {
        def sourcePath = sourceFile.absolutePath
        def indexOfLeadFromSourcePath = sourcePath.lastIndexOf('src/main/java/')
        def shortSourcePath = sourcePath.substring(indexOfLeadFromSourcePath + 'src/main/java/'.length())
        return (shortSourcePath - '.java').replace('/', '.')
    }


    @CompileStatic
    File writeSimpleService(String packageDotted, File baseDir) {
        def path = 'src/main/java/' + packageDotted.replace('.', '/') + '/SimpleService.java'
        def javaFile = createFile(path, baseDir)
        javaFile << """
            package ${packageDotted};

            import org.apache.felix.scr.annotations.Component;
            import org.apache.felix.scr.annotations.Properties;
            import org.apache.felix.scr.annotations.Property;
            import org.apache.felix.scr.annotations.Reference;
            import org.apache.felix.scr.annotations.Service;

            @Component(
                label = "Simple Service Test",
                description = "Simple Service Test description",
                metatype = true,
                immediate = true
            )
            @Properties({
                @Property(
                    name = "testProp",
                    value = "testProp value",
                    propertyPrivate = true
                ),
                @Property(
                    label = "Vendor",
                    name = "testProp2",
                    value = "Time Warner Cable",
                    propertyPrivate = false
                ),
                @Property(
                    label = "Workflow Label",
                    name = "process.label",
                    value = "Publish to Stage Workflow Process",
                    description = "Label which will appear in the Adobe CQ Workflow interface"
                )
            })
            @Service
            public class SimpleService implements Runnable {
                @Property(label = "Prop on constant")
                private static final String ENABLE_DATA_SYNCH = "prop.on.constant";

                @Reference(name = "myRunnableName")
                private Runnable myRunnable;

                public void run() {
                }
            }
        """.stripIndent()
    }


    @CompileStatic
    File writeSimpleServlet(String packageDotted, boolean useInterface, File baseDir) {
        def path = 'src/main/java/' + packageDotted.replace('.', '/') + '/SimpleServlet.java'
        def javaFile = createFile(path, baseDir)
        javaFile << """
            package ${packageDotted};

            import org.apache.felix.scr.annotations.Properties;
            import org.apache.felix.scr.annotations.Property;
            import org.apache.felix.scr.annotations.Reference;
            import org.apache.felix.scr.annotations.sling.SlingServlet;

            import javax.servlet.Servlet;
            import javax.servlet.ServletConfig;
            import javax.servlet.ServletException;
            import javax.servlet.ServletRequest;
            import javax.servlet.ServletResponse;
            import java.io.IOException;

            @SlingServlet(
                methods = {"GET", "POST"},
                paths = {"/bin/add"}
            )
            @Properties({
                @Property(name = "service.vendor", value = "Time Warner Cable", propertyPrivate = false),
                @Property(name = "service.description", value = "Call to add offers to present to user's shopping cart session", propertyPrivate = false)
            })
            public class SimpleServlet implements Servlet {
                @Reference
                private ${useInterface ? "Runnable" : "SimpleService"} simpleService;


                @Override
                public void init(ServletConfig servletConfig) throws ServletException {
                }


                @Override
                public ServletConfig getServletConfig() {
                    return null;
                }


                @Override
                public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
                }


                @Override
                public String getServletInfo() {
                    return null;
                }


                @Override
                public void destroy() {
                }

            }
        """.stripIndent()
    }

}
