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

        stage('X-Platform Tests') {
            parallel {

                stage('Windows Java 8') {
                    agent {
                        node {
                            label 'windows && x64'
                            customWorkspace "${BUILD_TAG}-win8"
                        }
                    }
                    tools {
                        jdk 'jdk8'
                    }
                    steps {
                        bat 'mvn clean package -PnoJava9'
                    }
                    post {
                        always {
                            cleanWs()
                        }
                    }
                }

                stage('Linux Java 8') {
                    agent {
                        node {
                            label 'linux && x64'
                            customWorkspace "${BUILD_TAG}-lin8"
                        }
                    }
                    tools {
                        jdk 'jdk8'
                    }
                    steps {
                        sh 'mvn clean package -PnoJava9'
                    }
                    post {
                        always {
                            cleanWs()
                        }
                    }
                }

                stage('OSX Java 8') {
                    agent {
                        node {
                            label 'osx && x64'
                            customWorkspace "${BUILD_TAG}-osx8"
                        }
                    }
                    tools {
                        jdk 'jdk8'
                    }
                    steps {
                        sh 'mvn clean package -PnoJava9'
                    }
                    post {
                        always {
                            cleanWs()
                        }
                    }
                }

                stage('Windows Java 9') {
                    agent {
                        node {
                            label 'windows && x64'
                            customWorkspace "${BUILD_TAG}-win9"
                        }
                    }
                    tools {
                        jdk 'jdk9'
                    }
                    steps {
                        bat 'mvn clean verify'
                    }
                    post {
                        always {
                            cleanWs()
                        }
                    }
                }

                stage('Linux Java 9') {
                    agent {
                        node {
                            label 'linux && x64'
                            customWorkspace "${BUILD_TAG}-lin9"
                        }
                    }
                    tools {
                        jdk 'jdk9'
                    }
                    steps {
                        sh 'mvn clean verify'
                    }
                    post {
                        always {
                            cleanWs()
                        }
                    }
                }

                stage('OSX Java 9') {
                    agent {
                        node {
                            label 'osx && x64'
                            customWorkspace "${BUILD_TAG}-osx9"
                        }
                    }
                    tools {
                        jdk 'jdk9'
                    }
                    steps {
                        sh 'mvn clean verify'
                    }
                    post {
                        always {
                            cleanWs()
                        }
                    }
                }

            }
        }

    }

    post {
        always {
            cleanWs()
        }
    }

}
