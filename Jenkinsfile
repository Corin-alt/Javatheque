pipeline {
    agent any

    triggers {
        githubPush()
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
        DB_URL = credentials('db_url')
        DB_USER = credentials('db_user')
        DB_PASSWORD = credentials('db_password')
    }
    
    tools {
        maven 'Maven'
        dockerTool 'Docker'
        jdk 'JDK17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_MSG = sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
                    env.GIT_AUTHOR = sh(returnStdout: true, script: 'git log -1 --pretty=%an').trim()
                }
            }
        }
        
        stage('Setup Environment') {
            steps {
                echo "Setup Environment..."
                script {
                    sh '''
                        if [ "$(id -u)" = 0 ]; then
                            apt-get update && apt-get install -y wget gnupg unzip
                        else
                            sudo apt-get update && sudo apt-get install -y wget gnupg unzip
                        fi
                        
                        wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | sudo apt-key add -
                        echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" | sudo tee /etc/apt/sources.list.d/google.list
                        sudo apt-get update
                        sudo apt-get install -y google-chrome-stable
                        
                        CHROME_VERSION=$(google-chrome --version | awk '{ print $3 }' | cut -d'.' -f1)
                        CHROMEDRIVER_VERSION=$(wget -qO- "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_VERSION}")
                        wget -N "https://chromedriver.storage.googleapis.com/${CHROMEDRIVER_VERSION}/chromedriver_linux64.zip"
                        unzip chromedriver_linux64.zip
                        sudo mv chromedriver /usr/local/bin/
                        sudo chmod +x /usr/local/bin/chromedriver
                    '''
                }
            }
        }
        
        stage('Install GlassFish') {
            steps {
                script {
                    sh '''
                        if [ ! -d "${GLASSFISH_HOME}" ]; then
                            wget https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                            unzip glassfish-7.0.0.zip -d /opt/
                            sudo chmod -R +x ${GLASSFISH_HOME}/bin
                        fi
                    '''
                }
            }
        }
    }
    
    post {
        always {
            sh '${GLASSFISH_HOME}/bin/asadmin stop-domain domain1 || true'
            cleanWs()
        }
        success {
            echo 'Pipeline successfully executed!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}