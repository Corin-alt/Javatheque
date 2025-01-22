pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        buildDiscarder logRotator(
            artifactDaysToKeepStr: '', 
            artifactNumToKeepStr: '', 
            daysToKeepStr: '', 
            numToKeepStr: '2'
        )
    }

    environment {
        APP_NAME = 'javatheque'
        DOCKER_IMAGE = 'javatheque-env'
        DOCKER_TAG = 'latest'
        DOCKER_REGISTRY = 'ghcr.io'
        GITHUB_OWNER = 'corin-alt'

        DEPLOY_SERVER = credentials('deploy-server')
        APP_CODE_PATH = '/apps/java/src'
        APP_DEPLOY_PATH = '/apps/java/deploy'

        DOCKERFILE_CHANGED = 'false'

        GLASSFISH_HOME = '/opt/glassfish7'
        CHROME_OPTIONS = '--headless --no-sandbox --disable-dev-shm-usage'

        // DB_URL = credentials('db_url')        // URL de la base de données.
        // DB_USER = credentials('db_user')      // Nom d'utilisateur de la base de données
        // DB_PASSWORD = credentials('db_password')  // Mot de passe de la base de données
    }

    tools {
        maven 'Maven'
        dockerTool 'Docker'
        jdk 'JDK17'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    node {
                        echo "Cloning the repository..."
                        checkout scm
                        env.GIT_COMMIT_MSG = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
                        env.GIT_AUTHOR = sh(returnStdout: true, script: 'git log -1 --pretty=%an').trim()
                        echo "Git commit message: ${env.GIT_COMMIT_MSG}"
                        echo "Git author: ${env.GIT_AUTHOR}"
                    }
                }
            }
        }

        stage('Setup Environment') {
            steps {
                script {
                    node {
                        echo "Setting up Environment..."
                        sh '''
                            set -e
                            # Mise à jour des paquets et installation de dépendances
                            apt-get update
                            apt-get install -y wget gnupg unzip

                            # Installation de Google Chrome
                            wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
                            echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google.list
                            apt-get update
                            apt-get install -y google-chrome-stable

                            # Installation de ChromeDriver
                            CHROME_VERSION=$(google-chrome --version | awk '{ print $3 }' | cut -d'.' -f1)
                            wget -N "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_VERSION}"
                            wget -N "https://chromedriver.storage.googleapis.com/$(cat LATEST_RELEASE_${CHROME_VERSION})/chromedriver_linux64.zip"
                            unzip chromedriver_linux64.zip
                            mv chromedriver /usr/local/bin/
                            chmod +x /usr/local/bin/chromedriver

                            # Installation de GlassFish 7
                            wget https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                            unzip glassfish-7.0.0.zip -d /opt/
                            chmod -R +x ${GLASSFISH_HOME}/bin
                            echo "GlassFish installed successfully."
                        '''
                    }
                }
            }
        }

        stage('Verify Setup') {
            steps {
                script {
                    node {
                        echo "Verifying installations..."
                        sh '''
                            google-chrome --version || echo "Google Chrome installation failed"
                            ${GLASSFISH_HOME}/bin/asadmin version || echo "GlassFish installation failed"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                node {
                    echo "Cleaning up workspace and stopping services..."
                    sh '''
                        ${GLASSFISH_HOME}/bin/asadmin stop-domain domain1 || echo "Failed to stop GlassFish domain"
                    '''
                    cleanWs()
                }
            }
        }
        success {
            echo 'Pipeline successfully executed!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
