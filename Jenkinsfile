pipeline {
    agent {
        docker {
            image 'maven:3.8.4-openjdk-17-slim'
            args '-u root --privileged'
        }
    }

    triggers {
        githubPush()
    }

    environment {
        GLASSFISH_HOME = '/opt/glassfish7'
        PATH = "${env.GLASSFISH_HOME}/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Préparation de l\'environnement') {
            steps {
                sh '''
                    apt-get clean
                    rm -rf /var/lib/apt/lists/*
                    apt-get update -o Acquire::ForceHash=yes
                    
                    apt-get install -y \
                        wget \
                        unzip \
                        gnupg

                    wget https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                    unzip glassfish-7.0.0.zip -d /opt/
                    chmod -R +x ${GLASSFISH_HOME}/bin
                '''
            }
        }

        stage('Installation des dépendances') {
            steps {
                sh '''
                    apt-get install -y \
                        chromium \
                        chromium-driver
                '''
            }
        }

        stage('Build et Tests') {
            steps {
                sh 'mvn clean verify'
            }
        }

        stage('Démarrage de GlassFish') {
            steps {
                sh '''
                    ${GLASSFISH_HOME}/bin/asadmin start-domain domain1
                    sleep 10
                    ${GLASSFISH_HOME}/bin/asadmin version
                '''
            }
        }
    }

    post {
        always {
            sh '''
                if ${GLASSFISH_HOME}/bin/asadmin list-domains | grep -q running; then
                    ${GLASSFISH_HOME}/bin/asadmin stop-domain domain1
                fi
            '''
            cleanWs()
        }
        success {
            echo 'Pipeline réussi !'
        }
        failure {
            echo 'Pipeline échoué !'
        }
    }
}