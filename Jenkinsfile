pipeline {

    agent {
        node {
            label 'linux && x64'
            customWorkspace "${BUILD_TAG}" // default workspace may have "@" in it which can break file paths
        }
    }

    options {
        timeout(time: 20, unit: 'MINUTES')
    }

    tools {
        maven 'mvn3.3.9'
    }

    stages {

        stage("Java8") {
            tools {
                jdk 'jdk8'
            }
            steps {
                sh "mvn clean package"
            }
        }

    }

    post {
        always {
            cleanWs()
        }
    }

}
