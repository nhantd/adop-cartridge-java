package com.java.cartridge

import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.MemoryJobManagement
import spock.lang.Specification
import spock.lang.Shared

/**
 * Tests that EnvironmentProvisioning/CreateEnvironment job works as expected.
 */
class CreateEnvironmentJobSpec extends Specification {

    private static final file = new File('jenkins/jobs/dsl/environment_provisioning.groovy')

    private static final String workspaceName = 'ExampleWorkspace'
    private static final String projectName = "${workspaceName}/ExampleProject"
    private static final String environmentTemplateGitUrl = "ssh://jenkins@gerrit:29418/${projectName}/adop-cartridge-java-environment-template"

    @Shared MemoryJobManagement jm = getMemoryJobManagement()
    @Shared GeneratedItems items = DslScriptLoader.runDslEngine(file.text, jm)

    def 'Create_Environment job is exists'() {
        expect:
            jm.savedConfigs[jobName] != null

        where:
            jobName = "$projectName/Create_Environment"
    }

    def 'job parameters exists'() {
        expect:
            node.properties.size() == 1
            node.properties[0].children()[0].name() == 'hudson.model.ParametersDefinitionProperty'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def '"ENVIRONMENT_NAME" string parameter with "CI" as default value exists'() {
        expect:
            node.properties[0].children()[0].parameterDefinitions.size() == 1
            node.properties[0].children()[0].parameterDefinitions[0].children()[0].name() == 'hudson.model.StringParameterDefinition'

            node.properties[0].children()[0].parameterDefinitions[0].children()[0].name.size() == 1
            node.properties[0].children()[0].parameterDefinitions[0].children()[0].name[0].text() == 'ENVIRONMENT_NAME'

            node.properties[0].children()[0].parameterDefinitions[0].children()[0].defaultValue.size() == 1
            node.properties[0].children()[0].parameterDefinitions[0].children()[0].defaultValue[0].text() == 'CI'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'workspace_name and project_name env variables injected'() {
        expect:
            node.properties.size() == 1
            node.properties[0].EnvInjectJobProperty.size() == 1
            node.properties[0].EnvInjectJobProperty[0].info.size() == 1
            node.properties[0].EnvInjectJobProperty[0].info[0].propertiesContent.size() == 1
            node.properties[0].EnvInjectJobProperty[0].info[0].propertiesContent[0].text() == "WORKSPACE_NAME=${workspaceName}\nPROJECT_NAME=${projectName}"

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'job assigned to Docker node'() {
        expect:
            node.assignedNode.size() == 1
            node.assignedNode[0].text() == 'docker'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'wrappers exists'() {
        expect:
            node.buildWrappers.size() == 1

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'wrappers preBuildCleanup exists'() {
        expect:
            node.buildWrappers[0].children()[0].name() == 'hudson.plugins.ws__cleanup.PreBuildCleanup'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'wrappers injectPasswords exists'() {
        expect:
            node.buildWrappers[0].children()[1].name() == 'EnvInjectPasswordWrapper'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'wrappers maskPasswords exists'() {
        expect:
            node.buildWrappers[0].children()[2].name() == 'com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'wrappers sshAgent exists'() {
        expect:
            node.buildWrappers[0].children()[3].name() == 'com.cloudbees.jenkins.plugins.sshagent.SSHAgentBuildWrapper'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'wrappers sshAgent value adop-jenkins-master'() {
        expect:
            node.buildWrappers[0].children()[3].value()[0].value()[0] == 'adop-jenkins-master'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'steps with one shell block exists'() {
        expect:
            node.builders.size() == 1
            node.builders[0].children().size() == 1
            node.builders[0].children()[0].name() == 'hudson.tasks.Shell'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'scm block with settings exists'() {
        expect:
            node.scm.size() == 1
            node.scm[0].userRemoteConfigs.size() == 1
            node.scm[0].userRemoteConfigs[0].children()[0].name() == 'hudson.plugins.git.UserRemoteConfig'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'scm remote name is origin'() {
        expect:
            node.scm[0].userRemoteConfigs[0].children()[0].name.size() == 1
            node.scm[0].userRemoteConfigs[0].children()[0].name[0].text() == 'origin'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'scm remote url is equal to environmentTemplateGitUrl'() {
        expect:
            node.scm[0].userRemoteConfigs[0].children()[0].url.size() == 1
            node.scm[0].userRemoteConfigs[0].children()[0].url[0].text() == environmentTemplateGitUrl

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'scm credentials specified as adop-jenkins-master'() {
        expect:
            node.scm[0].userRemoteConfigs[0].children()[0].credentialsId.size() == 1
            node.scm[0].userRemoteConfigs[0].children()[0].credentialsId[0].text() == 'adop-jenkins-master'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'scm branch is */master'() {
        expect:
            node.scm[0].branches.size() == 1
            node.scm[0].branches[0].children()[0].name() == 'hudson.plugins.git.BranchSpec'
            node.scm[0].branches[0].children()[0].text() == '*/master'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'pipeline trigger exists'() {
        expect:
            node.publishers.size() == 1
            node.publishers[0].children()[0].name() == 'au.com.centrumsystems.hudson.plugin.buildpipeline.trigger.BuildPipelineTrigger'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'downstream trigger on "Destroy_Environment" job exists'() {
        expect:
            node.publishers[0].children()[0].downstreamProjectNames.size() == 1
            node.publishers[0].children()[0].downstreamProjectNames[0].text() == "$projectName/Destroy_Environment"

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
    }

    def 'downstream trigger on "Destroy_Environment" is parameterized with CurrentBuildParameters usage'() {
        expect:
            node.publishers[0].children()[0].configs.size() == 1
            node.publishers[0].children()[0].configs[0].children()[0].name() == 'hudson.plugins.parameterizedtrigger.CurrentBuildParameters'

        where:
            node = new XmlParser().parseText(jm.savedConfigs["$projectName/Create_Environment"])
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