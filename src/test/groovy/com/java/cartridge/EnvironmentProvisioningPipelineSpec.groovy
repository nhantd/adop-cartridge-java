package com.java.cartridge

import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests that EnvironmentProvisioning Pipeline View dsl works as expected.
 */
class EnvironmentProvisioningPipelineSpec extends Specification {

    @Shared
    def helper = new DslHelper('jenkins/jobs/dsl/environment_provisioning.groovy')

    @Shared
    def Node node = new XmlParser().parseText(helper.jm.savedViews["${helper.projectName}/Environment_Provisioning"])

    def 'view "Environment_Provisioning" exists'() {
        expect:
            helper.jm.savedViews[viewName] != null

        where:
            viewName = "${helper.projectName}/Environment_Provisioning"
    }

    def 'title of view is "Environment Provisioning Pipeline"'() {
        expect:
            node.buildViewTitle.size() == 1
            node.buildViewTitle[0].text() == 'Environment Provisioning Pipeline'
    }

    def 'first trigger on job in view is "Create_Environment"'() {
        expect:
            node.selectedJob.size() == 1
            node.selectedJob[0].text() == 'Create_Environment'
    }

    def 'number of display builds in view is 5'() {
        expect:
            node.noOfDisplayedBuilds.size() == 1
            node.noOfDisplayedBuilds[0].text() == '5'
    }
}
