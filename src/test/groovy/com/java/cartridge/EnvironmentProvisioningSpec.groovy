package com.java.cartridge

import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.MemoryJobManagement
import spock.lang.Specification
import spock.lang.Shared

// @TODO:
// - Hide fixtures like: loading file, setting env variables into setupSpec
// - Add proper tests on variables, because even if we provide wrong PROJECT_NAME (without WORKSPACE_NAME prefix) - our tests passed which is wrong
// - Rewrite Java list on Groovy list, i mean List<String> - change on something else...
// - Add real tests on content of jobs, as starting point take a look on - https://github.com/jenkinsci/job-dsl-plugin/blob/master/job-dsl-core/src/test/groovy/javaposse/jobdsl/dsl/DslSampleSpec.groovy

/**
 * Tests that EnvironmentProvisioning dsl works as expected.
 */
class EnvironmentProvisioningSpec extends Specification {

    private static final file = new File('jenkins/jobs/dsl/environment_provisioning.groovy')

    private static final String workspaceName = 'ExampleWorkspace'
    private static final String projectName = 'ExampleProject'
    private static final String projectFolderName = "$workspaceName/$projectName"

    List<String> expectedJobs = [
        "$projectName/Create_Environment",
        "$projectName/Destroy_Environment",
        "$projectName/List_Environment"
    ]

    List<String> expectedViews = [
        "$projectName/Environment_Provisioning"
    ]

    @Shared MemoryJobManagement jm = getMemoryJobManagement()
    @Shared GeneratedItems items = DslScriptLoader.runDslEngine(file.text, jm)

    def 'load jobs'() {
        expect:
            items.getJobs() != null
            items.getJobs().size() == expectedJobs.size()
    }

    def 'load views'() {
        expect:
            items.getViews() != null
            items.getViews().size() == expectedViews.size()
    }

    def 'should generate exactly the expected jobs'() {
        expect:
            actualJobs == expectedJobs

        where:
            actualJobs = items.getJobs().jobName
    }

    def 'should generate exactly the expected views'() {
        expect:
            actualViews == expectedViews

        where:
            actualViews = items.getViews().name
    }

    def 'List_Environment is exists'() {
        expect:
            jm.savedConfigs[jobName] != null

        where:
            jobName = "$projectName/List_Environment"
    }

    def 'List_Environment assigned to docker node'() {
        expect:
            node.assignedNode.size() == 1
            node.assignedNode[0].text() == 'docker'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'List_Environment description'() {
        expect:
            node.description.size() == 1
            node.description[0].text() == 'This job list the running environments for project'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'List_Environment wrappers preBuildCleanup'() {
        expect:
            node.buildWrappers.size() == 1
            node.buildWrappers[0].children()[0].name() == 'hudson.plugins.ws__cleanup.PreBuildCleanup'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'List_Environment wrappers injectPasswords'() {
        expect:
            node.buildWrappers.size() == 1
            node.buildWrappers[0].children()[1].name() == 'EnvInjectPasswordWrapper'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'List_Environment wrappers maskPasswords'() {
        expect:
            node.buildWrappers.size() == 1
            node.buildWrappers[0].children()[2].name() == 'com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'List_Environment wrappers sshAgent'() {
        expect:
            node.buildWrappers.size() == 1
            node.buildWrappers[0].children()[3].name() == 'com.cloudbees.jenkins.plugins.sshagent.SSHAgentBuildWrapper'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'List_Environment wrappers sshAgent value adop-jenkins-master'() {
        expect:
            node.buildWrappers.size() == 1
            node.buildWrappers[0].children()[3].value()[0].value()[0] == 'adop-jenkins-master'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'List_Environment with one shell step'() {
        expect:
            node.builders.size() == 1
            node.builders[0].children()[0].name() == 'hudson.tasks.Shell'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/List_Environment"])
    }

    def 'check view "Environment_Provisioning" is exists'() {
        expect:
            jm.savedViews[viewName] != null

        where:
            viewName = "$projectName/Environment_Provisioning"
    }

    def 'title of view should be to "Environment Provisioning Pipeline"'() {
        expect:
            node.buildViewTitle.size() == 1
            node.buildViewTitle[0].text() == 'Environment Provisioning Pipeline'

        where:
            node = new XmlParser().parseText(jm.savedViews["$projectName/Environment_Provisioning"])
    }

    def 'check trigger on job in view is "Create_Environment"'() {
        expect:
            node.selectedJob.size() == 1
            node.selectedJob[0].text() == 'Create_Environment'

        where:
            node = new XmlParser().parseText(jm.savedViews["$projectName/Environment_Provisioning"])
    }

    def 'check number of display builds in view is 5'() {
        expect:
            node.noOfDisplayedBuilds.size() == 1
            node.noOfDisplayedBuilds[0].text() == '5'

        where:
            node = new XmlParser().parseText(jm.savedViews["$projectName/Environment_Provisioning"])
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
