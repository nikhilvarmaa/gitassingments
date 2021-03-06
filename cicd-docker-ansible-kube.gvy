pipeline {
agent any
stages {
    stage('compile') {
	    steps { 
		    echo 'compiling..'
		    git url: 'https://github.com/sandipdabre/devops_project'
		    sh script: '/opt/apache-maven-3.8.4/bin/mvn compile'
	    }
    }
    stage('codereview-pmd') {
	    steps { 
		    echo 'codereview..'
		    sh script: '/opt/apache-maven-3.8.4/bin/mvn -P metrics pmd:pmd'
            }
	    post {
		    success {
			    recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
		    }
	    }		
    }
    stage('unit-test') {
	    steps {
		    echo 'unittest..'
		    sh script: '/opt/apache-maven-3.8.4/bin/mvn test'
	    }
	    post {
		    success {
			    junit 'target/surefire-reports/*.xml'
		    }
	    }			
    }
    stage('codecoverage') {
	    steps {
		    echo 'codecoverage..'
		    sh script: '/opt/apache-maven-3.8.4/bin/mvn cobertura:cobertura -Dcobertura.report.format=xml'
	    }
	    post {
		    success {
			    cobertura autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: 'target/site/cobertura/coverage.xml', conditionalCoverageTargets: '70, 0, 0', failUnhealthy: false, failUnstable: false, lineCoverageTargets: '80, 0, 0', maxNumberOfBuilds: 0, methodCoverageTargets: '80, 0, 0', onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false                  
		    }
	    }		
    }
    stage('package/build-war') {
	    steps {
		    echo 'package......'
		    sh script: '/opt/apache-maven-3.8.4/bin/mvn package'	
	    }		
    }
    stage('build & push docker image') {
	    steps {
		    sh 'cd $WORKSPACE'
		    sh 'sudo docker build --file Dockerfile --tag sandipdabre/devops_project:$BUILD_NUMBER .'
		    withCredentials([string(credentialsId: 'DOCKER_HUB_PWD', variable: 'DOCKER_HUB_PWD')]) {
			    sh "sudo docker login -u lerndevops -p ${DOCKER_HUB_PWD}"
		    }
		    sh 'sudo docker push sandipdabre/devops_project:$BUILD_NUMBER'
	    }
    }
    stage('Deploy-QA') {
	    steps {
		    sh 'sudo ansible-playbook --inventory /root/myinv deploy/deploy-kube.yml --extra-vars "env=qa build=$BUILD_NUMBER"'
	    }
    }
}
}
