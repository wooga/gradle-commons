package com.wooga.gradle

import nebula.test.IntegrationSpec
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.junit.Rule
import org.junit.contrib.java.lang.system.ProvideSystemProperty

import java.lang.reflect.ParameterizedType
import java.nio.file.Files

class MockIntegrationSpec extends IntegrationSpec {

    @Rule
    ProvideSystemProperty properties = new ProvideSystemProperty("ignoreDeprecations", "true")

    def setup() {
        applyPlugin(MockPlugin)
    }

    static File generateBatchWrapper(String fileName, Boolean printEnvironment = false) {
        File wrapper

        wrapper = Files.createTempFile(fileName, ".bat").toFile()
        wrapper.deleteOnExit()
        wrapper.executable = true
        if (PlatformUtils.windows) {
            wrapper << """
                    @echo off
                    echo [ARGUMENTS]:
                    echo %*
                """.stripIndent()

            if (printEnvironment) {
                wrapper << """
                    echo [ENVIRONMENT]:
                    set
                """.stripIndent()
            }

        } else {
            wrapper << """
                    #!/usr/bin/env bash
                    echo [ARGUMENTS]:
                    echo \$@
                """.stripIndent()

            if (printEnvironment) {
                wrapper << """
                    echo [ENVIRONMENT]:
                    env
                """.stripIndent()
            }
        }

        wrapper
    }
}

class MockTaskIntegrationSpec<T extends MockTask> extends MockIntegrationSpec {

    Class<T> getSubjectUnderTestClass() {
        if (!_sutClass) {
            try {
                this._sutClass = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass())
                        .getActualTypeArguments()[0];
            }
            catch (Exception e) {
                this._sutClass = (Class<T>) MockTask
            }
        }
        _sutClass
    }
    private Class<T> _sutClass

    String getSubjectUnderTestName() {
        "${subjectUnderTestClass.simpleName.uncapitalize()}"
    }

    String getSubjectUnderTestTypeName() {
        subjectUnderTestClass.getTypeName()
    }

    def setup(){
        addMockTask(false)
    }

    void addMockTask(Boolean force, String... lines) {
        addTask(subjectUnderTestName, subjectUnderTestTypeName, force, lines)
    }

    void appendToSubjectTask(String... lines) {
        buildFile << """
        $subjectUnderTestName {
            ${lines.join('\n')}
        }
        """.stripIndent()
    }

    def runSubjectTaskSuccessfully() {
        runTasksSuccessfully(subjectUnderTestName)
    }

    void addTask(String name, String typeName, Boolean force, String... lines) {
        lines = lines ?: []
        buildFile << """
        task (${name}, type: ${typeName}) {                       
            ${force ? "onlyIf = {true}\n" : ""}${lines.join('\n')}
        }
        """.stripIndent()
    }
}

class MockPlugin implements Plugin<Project> {

    static final String extensionName = "commons"
    static final String taskName = "mock"

    @Override
    void apply(Project project) {
        def extension = project.extensions.create(MockExtension, extensionName, MockExtension)
        project.tasks.withType(MockTask).configureEach { t ->
        }
    }
}

class MockExtension {
}

class MockTask extends DefaultTask {
}
