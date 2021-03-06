// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def referenceAppgitRepo = "spring-petclinic"
def regressionTestGitRepo = "adop-cartridge-java-regression-tests"
def referenceAppGitUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + referenceAppgitRepo
def regressionTestGitUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + regressionTestGitRepo

// Jobs
def buildAppJob = freeStyleJob(projectFolderName + "/Reference_Application_Build")
def unitTestJob = freeStyleJob(projectFolderName + "/Reference_Application_Unit_Tests")
def codeAnalysisJob = freeStyleJob(projectFolderName + "/Reference_Application_Code_Analysis")
def deployJob = freeStyleJob(projectFolderName + "/Reference_Application_Deploy")
def regressionTestJob = freeStyleJob(projectFolderName + "/Reference_Application_Regression_Tests")
def performanceTestJob = freeStyleJob(projectFolderName + "/Reference_Application_Performance_Tests")
def deployJobToProdA = freeStyleJob(projectFolderName + "/Reference_Application_Deploy_ProdA")
def deployJobToProdB = freeStyleJob(projectFolderName + "/Reference_Application_Deploy_ProdB")

// Views
def pipelineView = buildPipelineView(projectFolderName + "/Java_Reference_Application")

pipelineView.with{
    title('Reference Application Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + "/Reference_Application_Build")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}

buildAppJob.with{
  description("This job builds Java Spring reference application")
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  scm{
    git{
      remote{
        url(referenceAppGitUrl)
        credentials("adop-jenkins-master")
      }
      branch("*/master")
    }
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("java8")
  triggers{
    gerrit{
      events{
        refUpdated()
      }
      configure { gerritxml ->
        gerritxml / 'gerritProjects' {
          'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject' {
            compareType("PLAIN")
            pattern(projectFolderName + "/" + referenceAppgitRepo)
            'branches' {
              'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.Branch' {
                compareType("PLAIN")
                pattern("master")
              }
            }
          }
        }
        gerritxml / serverName("ADOP Gerrit")
      }
    }
  }
  steps {
    maven{
      goals('clean install -DskipTests')
      mavenInstallation("ADOP Maven")
    }
  }
  publishers{
    archiveArtifacts("**/*")
    downstreamParameterized{
      trigger(projectFolderName + "/Reference_Application_Unit_Tests"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${BUILD_NUMBER}')
          predefinedProp("PARENT_BUILD", '${JOB_NAME}')
        }
      }
    }
  }
}

unitTestJob.with{
  description("This job runs unit tests on Java Spring reference application.")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("java8")
  steps {
  }
  steps {
    copyArtifacts("Reference_Application_Build") {
        buildSelector {
          buildNumber('${B}')
      }
    }
    maven{
      goals('clean test')
      mavenInstallation("ADOP Maven")
    }
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Reference_Application_Code_Analysis"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD",'${PARENT_BUILD}')
        }
      }
    }
  }
}

codeAnalysisJob.with{
  description("This job runs code quality analysis for Java reference application using SonarQube.")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  label("java8")
  steps {
    copyArtifacts('Reference_Application_Build') {
        buildSelector {
          buildNumber('${B}')
      }
    }
  }
  configure { myProject ->
    myProject / builders << 'hudson.plugins.sonar.SonarRunnerBuilder'(plugin:"sonar@2.2.1"){
      project('sonar-project.properties')
      properties('''sonar.projectKey=org.java.reference-application
sonar.projectName=Reference application
sonar.projectVersion=1.0.0
sonar.sources=src
sonar.language=java
sonar.sourceEncoding=UTF-8
sonar.scm.enabled=false''')
      javaOpts()
      jdk('(Inherit From Job)')
      task()
    }
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Reference_Application_Deploy"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD", '${PARENT_BUILD}')
        }
      }
    }
  }
}

deployJob.with{
  description("This job deploys the java reference application to the CI environment")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
    stringParam("ENVIRONMENT_NAME","CI","Name of the environment.")
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }

  configure { project ->
          project / 'properties'/'EnvInjectJobProperty'/ info <<{
                 groovyScriptContent  'matcher = JENKINS_URL =~ /http:\\/\\/(.*?)\\/jenkins.*/; def map = [STACK_IP: matcher[0][1]]; return map;'
          }
  }

  label("docker")
  steps {
    copyArtifacts("Reference_Application_Build") {
        buildSelector {
          buildNumber('${B}')
      }
    }
    shell('''set +x
            |export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
            |docker cp ${WORKSPACE}/target/petclinic.war  ${SERVICE_NAME}:/usr/local/tomcat/webapps/
            |docker restart ${SERVICE_NAME}
            |COUNT=1
            |while ! curl -q http://${SERVICE_NAME}:8080/petclinic -o /dev/null
            |do
            |  if [ ${COUNT} -gt 10 ]; then
            |      echo "Docker build failed even after ${COUNT}. Please investigate."
            |      exit 1
            |  fi
            |  echo "Application is not up yet. Retrying ..Attempt (${COUNT})"
            |  sleep 5
            |  COUNT=$((COUNT+1))
            |done
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "Environment URL: http://${SERVICE_NAME}.${STACK_IP}.xip.io/petclinic"
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |set -x'''.stripMargin())
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Reference_Application_Regression_Tests"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD", '${PARENT_BUILD}')
          predefinedProp("ENVIRONMENT_NAME", '${ENVIRONMENT_NAME}')
        }
      }
    }
  }
}

regressionTestJob.with{
  description("This job runs regression tests on deployed java application")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
    stringParam("ENVIRONMENT_NAME","CI","Name of the environment.")
  }
  scm{
    git{
      remote{
        url(regressionTestGitUrl)
        credentials("adop-jenkins-master")
      }
      branch("*/master")
    }
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  label("java8")
  steps {
    shell('''
            |export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
            |echo "SERVICE_NAME=${SERVICE_NAME}" > env.properties
            |
            |echo "Running automation tests"
            |echo "Setting values for container, project and app names"
            |CONTAINER_NAME="owasp_zap-"${SERVICE_NAME}${BUILD_NUMBER}
            |APP_IP=$( docker inspect --format '{{ .NetworkSettings.Networks.'"$DOCKER_NETWORK_NAME"'.IPAddress }}' ${SERVICE_NAME} )
            |APP_URL=http://${APP_IP}:8080/petclinic
            |ZAP_PORT="9090"
            |
            |echo CONTAINER_NAME=$CONTAINER_NAME >> env.properties
            |echo APP_URL=$APP_URL >> env.properties
            |echo ZAP_PORT=$ZAP_PORT >> env.properties
            |
            |echo "Starting OWASP ZAP Intercepting Proxy"
            |JOB_WORKSPACE_PATH="/var/lib/docker/volumes/jenkins_slave_home/_data/ExampleWorkspace/ExampleProject/Reference_Application_Regression_Tests"
            |#JOB_WORKSPACE_PATH="$(docker inspect --format '{{ .Mounts.Networks.'"$DOCKER_NETWORK_NAME"'.IPAddress }}' ${CONTAINER_NAME} )/${JOB_NAME}"
            |echo JOB_WORKSPACE_PATH=$JOB_WORKSPACE_PATH >> env.properties
            |mkdir -p ${JOB_WORKSPACE_PATH}/owasp_zap_proxy/test-results
            |docker run -it -d --net=$DOCKER_NETWORK_NAME -v ${JOB_WORKSPACE_PATH}/owasp_zap_proxy/test-results/:/opt/zaproxy/test-results/ --name ${CONTAINER_NAME} -P nhantd/owasp_zap start zap-test
            |
            |sleep 5s
            |ZAP_IP=$( docker inspect --format '{{ .NetworkSettings.Networks.'"$DOCKER_NETWORK_NAME"'.IPAddress }}' ${CONTAINER_NAME} )
            |echo "ZAP_IP =  $ZAP_IP"
            |echo ZAP_IP=$ZAP_IP >> env.properties
            |echo ZAP_ENABLED="true" >> env.properties
            |echo "Running Selenium tests through maven."
            |'''.stripMargin())
    environmentVariables {
      propertiesFile('env.properties')
    }
    maven{
      goals('clean -B test -DPETCLINIC_URL=${APP_URL} -DZAP_IP=${ZAP_IP} -DZAP_PORT=${ZAP_PORT} -DZAP_ENABLED=${ZAP_ENABLED}')
      mavenInstallation("ADOP Maven")
    }
    shell('''
            |echo "Stopping OWASP ZAP Proxy and generating report."
            |docker stop ${CONTAINER_NAME}
            |docker rm ${CONTAINER_NAME}
            |
            |docker run -i --net=$DOCKER_NETWORK_NAME -v ${JOB_WORKSPACE_PATH}/owasp_zap_proxy/test-results/:/opt/zaproxy/test-results/ --name ${CONTAINER_NAME} -P nhantd/owasp_zap stop zap-test
            |docker cp ${CONTAINER_NAME}:/opt/zaproxy/test-results/zap-test-report.html .
            |sleep 10s
            |docker rm ${CONTAINER_NAME}
            |'''.stripMargin())
  }
  configure{myProject ->
    myProject / 'publishers' << 'net.masterthought.jenkins.CucumberReportPublisher'(plugin:'cucumber-reports@0.1.0'){
      jsonReportDirectory("")
      pluginUrlPath("")
      fileIncludePattern("")
      fileExcludePattern("")
      skippedFails("false")
      pendingFails("false")
      undefinedFails("false")
      missingFails("false")
      noFlashCharts("false")
      ignoreFailedTests("false")
      parallelTesting("false")
    }
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/Reference_Application_Performance_Tests"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          predefinedProp("B",'${B}')
          predefinedProp("PARENT_BUILD", '${PARENT_BUILD}')
          predefinedProp("ENVIRONMENT_NAME", '${ENVIRONMENT_NAME}')
        }
      }
    }
    publishHtml {
        report('$WORKSPACE') {
            reportName('ZAP security test report')
            reportFiles('zap-test-report.html')
            }
        }
    }
}

performanceTestJob.with{
  description("This job run the Jmeter test for the java reference application")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Regression_Tests","Parent build name")
    stringParam("ENVIRONMENT_NAME","CI","Name of the environment.")
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
      env('JMETER_TESTDIR','jmeter-test')
  }
  label("docker")
  steps {
    copyArtifacts("Reference_Application_Build") {
      buildSelector {
          buildNumber('${B}')
      }
      targetDirectory('${JMETER_TESTDIR}')
    }
      shell('''export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
            |if [ -e ../apache-jmeter-2.13.tgz ]; then
            |	cp ../apache-jmeter-2.13.tgz $JMETER_TESTDIR
            |else
            |	wget http://www.apache.org/dist/jmeter/binaries/apache-jmeter-2.13.tgz
            |    cp apache-jmeter-2.13.tgz ../
            |    mv apache-jmeter-2.13.tgz $JMETER_TESTDIR
            |fi
            |cd $JMETER_TESTDIR
            |tar -xf apache-jmeter-2.13.tgz
            |echo 'Changing user defined parameters for jmx file'
            |sed -i 's/PETCLINIC_HOST_VALUE/'"${SERVICE_NAME}"'/g' src/test/jmeter/petclinic_test_plan.jmx
            |sed -i 's/PETCLINIC_PORT_VALUE/8080/g' src/test/jmeter/petclinic_test_plan.jmx
            |sed -i 's/CONTEXT_WEB_VALUE/petclinic/g' src/test/jmeter/petclinic_test_plan.jmx
            |sed -i 's/HTTPSampler.path"></HTTPSampler.path">petclinic</g' src/test/jmeter/petclinic_test_plan.jmx
            |'''.stripMargin())

      ant {
          props('testpath':'$WORKSPACE/$JMETER_TESTDIR/src/test/jmeter','test':'petclinic_test_plan')
          buildFile('${WORKSPACE}/$JMETER_TESTDIR/apache-jmeter-2.13/extras/build.xml')
          antInstallation('ADOP Ant')
      }

      shell('''|mv $JMETER_TESTDIR/src/test/gatling/* .
              |export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
              |CONTAINER_IP=$(docker inspect --format '{{ .NetworkSettings.Networks.'"$DOCKER_NETWORK_NAME"'.IPAddress }}' ${SERVICE_NAME})
              |sed -i "s/###TOKEN_VALID_URL###/http:\\/\\/${CONTAINER_IP}:8080/g" ${WORKSPACE}/src/test/scala/default/RecordedSimulation.scala
              |sed -i "s/###TOKEN_RESPONSE_TIME###/10000/g" ${WORKSPACE}/src/test/scala/default/RecordedSimulation.scala
                |'''.stripMargin())
      maven {
          goals('gatling:execute')
          mavenInstallation('ADOP Maven')
      }

  }
  publishers{
    publishHtml {
        report('$WORKSPACE/$JMETER_TESTDIR/src/test/jmeter') {
            reportName('Jmeter Report')
            reportFiles('petclinic_test_plan.html')
            }
        }
        buildPipelineTrigger(projectFolderName + "/Reference_Application_Deploy_ProdA") {
          parameters {
              predefinedProp("B",'${B}')
              predefinedProp("PARENT_BUILD",'${PARENT_BUILD}')
          }
      }
    }
    configure { project -> project / publishers << 'io.gatling.jenkins.GatlingPublisher' {
        enabled true
    	}
    }
}

deployJobToProdA.with{
  description("This job deploys the java reference application to the ProdA environment")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
    stringParam("ENVIRONMENT_NAME","PRODA","Name of the environment.")
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }

  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  configure { project ->
          project / 'properties'/'EnvInjectJobProperty'/ info <<{
                 groovyScriptContent  'matcher = JENKINS_URL =~ /http:\\/\\/(.*?)\\/jenkins.*/; def map = [STACK_IP: matcher[0][1]]; return map;'
          }
  }

  label("docker")
  steps {
    copyArtifacts("Reference_Application_Build") {
        buildSelector {
          buildNumber('${B}')
      }
    }
    shell('''
            |export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
            |docker cp ${WORKSPACE}/target/petclinic.war  ${SERVICE_NAME}:/usr/local/tomcat/webapps/
            |docker restart ${SERVICE_NAME}
            |COUNT=1
            |while ! curl -q http://${SERVICE_NAME}:8080/petclinic -o /dev/null
            |do
            |  if [ ${COUNT} -gt 10 ]; then
            |      echo "Docker build failed even after ${COUNT}. Please investigate."
            |      exit 1
            |  fi
            |  echo "Application is not up yet. Retrying ..Attempt (${COUNT})"
            |  sleep 5
            |  COUNT=$((COUNT+1))
            |done
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "Environment URL: http://${SERVICE_NAME}.${STACK_IP}.xip.io/petclinic"
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |set -x'''.stripMargin())
  }
  publishers{
      buildPipelineTrigger(projectFolderName + "/Reference_Application_Deploy_ProdB") {
        parameters {
            predefinedProp("B",'${B}')
            predefinedProp("PARENT_BUILD",'${PARENT_BUILD}')
            predefinedProp("ENVIRONMENT_PREVNODE",'${ENVIRONMENT_NAME}')
        }
    }
  }
}

deployJobToProdB.with{
  description("This job deploys the java reference application to the ProdA environment")
  parameters{
    stringParam("B",'',"Parent build number")
    stringParam("PARENT_BUILD","Reference_Application_Build","Parent build name")
    stringParam("ENVIRONMENT_NAME","PRODB","Name of the environment.")
  }
  wrappers {
    preBuildCleanup()
    injectPasswords()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  environmentVariables {
      env('WORKSPACE_NAME',workspaceFolderName)
      env('PROJECT_NAME',projectFolderName)
  }
  configure { project ->
          project / 'properties'/'EnvInjectJobProperty'/ info <<{
                 groovyScriptContent  'matcher = JENKINS_URL =~ /http:\\/\\/(.*?)\\/jenkins.*/; def map = [STACK_IP: matcher[0][1]]; return map;'
          }
  }
  label("docker")
  steps {
    copyArtifacts("Reference_Application_Build") {
        buildSelector {
          buildNumber('${B}')
      }
    }
    shell('''|export SERVICE_NAME="$(echo ${PROJECT_NAME} | tr '/' '_')_${ENVIRONMENT_NAME}"
            |docker cp ${WORKSPACE}/target/petclinic.war  ${SERVICE_NAME}:/usr/local/tomcat/webapps/
            |docker restart ${SERVICE_NAME}
            |COUNT=1
            |while ! curl -q http://${SERVICE_NAME}:8080/petclinic -o /dev/null
            |do
            |  if [ ${COUNT} -gt 10 ]; then
            |      echo "Docker build failed even after ${COUNT}. Please investigate."
            |      exit 1
            |  fi
            |  echo "Application is not up yet. Retrying ..Attempt (${COUNT})"
            |  sleep 5
            |  COUNT=$((COUNT+1))
            |done
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "Environment URL: http://${SERVICE_NAME}.${STACK_IP}.xip.io/petclinic"
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |echo "=.=.=.=.=.=.=.=.=.=.=.=."
            |set -x'''.stripMargin())
  }
}
