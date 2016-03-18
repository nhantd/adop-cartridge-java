package com.java.cartridge

import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that EnvironmentProvisioning/ListEnvironment job works as expected.
 */
class ListEnvironmentJobSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/environment_provisioning.groovy')

    @Shared
    def Node node = new XmlParser().parseText(helper.jm.savedConfigs["${helper.projectName}/List_Environment"])

    def 'List_Environment job is exists'() {
        expect:
            helper.jm.savedConfigs[jobName] != null

        where:
            jobName = "${helper.projectName}/List_Environment"
    }

    def 'job parameters not exists'() {
        expect:
            node.properties.size() == 1
            node.properties[0].children()[0].name() != 'hudson.model.ParametersDefinitionProperty'
    }

    def 'workspace_name and project_name env variables injected'() {
        expect:
            node.properties.size() == 1
            node.properties[0].EnvInjectJobProperty.size() == 1
            node.properties[0].EnvInjectJobProperty[0].info.size() == 1
            node.properties[0].EnvInjectJobProperty[0].info[0].propertiesContent.size() == 1
            node.properties[0].EnvInjectJobProperty[0].info[0].propertiesContent[0].text() == "WORKSPACE_NAME=${helper.workspaceName}\nPROJECT_NAME=${helper.projectName}"
    }

    def 'job assigned to Docker node'() {
        expect:
            node.assignedNode.size() == 1
            node.assignedNode[0].text() == 'docker'
    }

    def 'wrappers exists'() {
        expect:
            node.buildWrappers.size() == 1
    }

    def 'wrappers preBuildCleanup is used'() {
        expect:
            node.buildWrappers[0].children()[0].name() == 'hudson.plugins.ws__cleanup.PreBuildCleanup'
    }

    def 'wrappers injectPasswords is used'() {
        expect:
            node.buildWrappers[0].children()[1].name() == 'EnvInjectPasswordWrapper'
    }

    def 'wrappers maskPasswords is used'() {
        expect:
            node.buildWrappers[0].children()[2].name() == 'com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper'
    }

    def 'wrappers sshAgent is used'() {
        expect:
            node.buildWrappers[0].children()[3].name() == 'com.cloudbees.jenkins.plugins.sshagent.SSHAgentBuildWrapper'
    }

    def 'wrappers sshAgent value adop-jenkins-master'() {
        expect:
            node.buildWrappers[0].children()[3].value()[0].value()[0] == 'adop-jenkins-master'
    }

    def 'steps with one shell block exists'() {
        expect:
            node.builders.size() == 1
            node.builders[0].children().size() == 1
            node.builders[0].children()[0].name() == 'hudson.tasks.Shell'
    }
}