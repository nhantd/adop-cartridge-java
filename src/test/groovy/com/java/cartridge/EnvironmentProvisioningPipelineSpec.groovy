package com.java.cartridge

import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.MemoryJobManagement
import spock.lang.Specification
import spock.lang.Shared

/**
 * Tests that EnvironmentProvisioning Pipeline View dsl works as expected.
 */
class EnvironmentProvisioningPipelineSpec extends Specification {

    private static final file = new File('jenkins/jobs/dsl/environment_provisioning.groovy')

    private static final String workspaceName = 'ExampleWorkspace'
    private static final String projectName = "$workspaceName/ExampleProject"

    @Shared MemoryJobManagement jm = getMemoryJobManagement()
    @Shared GeneratedItems items = DslScriptLoader.runDslEngine(file.text, jm)

    def 'view "Environment_Provisioning" exists'() {
        expect:
            jm.savedViews[viewName] != null

        where:
            viewName = "$projectName/Environment_Provisioning"
    }

    def 'title of view is "Environment Provisioning Pipeline"'() {
        expect:
            node.buildViewTitle.size() == 1
            node.buildViewTitle[0].text() == 'Environment Provisioning Pipeline'

        where:
            node = new XmlParser().parseText(jm.savedViews["$projectName/Environment_Provisioning"])
    }

    def 'first trigger on job in view is "Create_Environment"'() {
        expect:
            node.selectedJob.size() == 1
            node.selectedJob[0].text() == 'Create_Environment'

        where:
            node = new XmlParser().parseText(jm.savedViews["$projectName/Environment_Provisioning"])
    }

    def 'number of display builds in view is 5'() {
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
