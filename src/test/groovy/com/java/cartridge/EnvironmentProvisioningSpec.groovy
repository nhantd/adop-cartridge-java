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
    private static final String projectName = "$workspaceName/ExampleProject"

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

    static def MemoryJobManagement getMemoryJobManagement() {
        MemoryJobManagement jm = new MemoryJobManagement()
        jm.parameters << [
            WORKSPACE_NAME: workspaceName,
            PROJECT_NAME  : projectName,
        ]
        return jm
    }
}
