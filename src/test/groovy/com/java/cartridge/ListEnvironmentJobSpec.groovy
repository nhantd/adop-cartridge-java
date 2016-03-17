package com.java.cartridge

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.MemoryJobManagement
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that EnvironmentProvisioning/ListEnvironment job works as expected.
 */
class ListEnvironmentJobSpec extends Specification {

    private static final file = new File('jenkins/jobs/dsl/environment_provisioning.groovy')

    private static final String workspaceName = 'ExampleWorkspace'
    private static final String projectName = "${workspaceName}/ExampleProject"
    private static final String environmentTemplateGitUrl = "ssh://jenkins@gerrit:29418/${projectName}/adop-cartridge-java-environment-template"

    @Shared MemoryJobManagement jm = getMemoryJobManagement()
    @Shared GeneratedItems items = DslScriptLoader.runDslEngine(file.text, jm)

    def 'List_Environment job is exists'() {
        expect:
            jm.savedConfigs[jobName] != null

        where:
            jobName = "$projectName/List_Environment"
    }

    def 'job parameters not exists'() {
        expect:
            node.properties.size() == 1
            node.properties[0].children()[0].name() != 'hudson.model.ParametersDefinitionProperty'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'workspace_name and project_name env variables injected'() {
        expect:
            node.properties.size() == 1
            node.properties[0].EnvInjectJobProperty.size() == 1
            node.properties[0].EnvInjectJobProperty[0].info.size() == 1
            node.properties[0].EnvInjectJobProperty[0].info[0].propertiesContent.size() == 1
            node.properties[0].EnvInjectJobProperty[0].info[0].propertiesContent[0].text() == "WORKSPACE_NAME=${workspaceName}\nPROJECT_NAME=${projectName}"

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'job assigned to Docker node'() {
        expect:
            node.assignedNode.size() == 1
            node.assignedNode[0].text() == 'docker'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'wrappers exists'() {
        expect:
            node.buildWrappers.size() == 1

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'wrappers preBuildCleanup exists'() {
        expect:
            node.buildWrappers[0].children()[0].name() == 'hudson.plugins.ws__cleanup.PreBuildCleanup'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'wrappers injectPasswords exists'() {
        expect:
            node.buildWrappers[0].children()[1].name() == 'EnvInjectPasswordWrapper'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'wrappers maskPasswords exists'() {
        expect:
            node.buildWrappers[0].children()[2].name() == 'com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'wrappers sshAgent exists'() {
        expect:
            node.buildWrappers[0].children()[3].name() == 'com.cloudbees.jenkins.plugins.sshagent.SSHAgentBuildWrapper'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'wrappers sshAgent value adop-jenkins-master'() {
        expect:
            node.buildWrappers[0].children()[3].value()[0].value()[0] == 'adop-jenkins-master'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'steps with one shell block exists'() {
        expect:
            node.builders.size() == 1
            node.builders[0].children().size() == 1
            node.builders[0].children()[0].name() == 'hudson.tasks.Shell'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    static def MemoryJobManagement getMemoryJobManagement() {
        MemoryJobManagement jm = new MemoryJobManagement()
        jm.parameters << [
            WORKSPACE_NAME: workspaceName,
            PROJECT_NAME  : projectName,
        ]
        return jm
    }
}