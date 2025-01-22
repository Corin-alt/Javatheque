pipeline {
    agent {
        docker {
            image 'ubuntu:latest'
            args '-u root'
        }
    }

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
        stage('Setup Environment..') {
            steps {
                script {
                    sh '''
                        apt-get update
                        apt-get install -y wget gnupg curl
                        wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
                        echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google.list
                        apt-get update
                        apt-get install -y google-chrome-stable
                        
                        CHROME_VERSION=$(google-chrome --version | awk '{ print $3 }' | cut -d'.' -f1)
                        wget -N "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_VERSION}"
                        wget -N "https://chromedriver.storage.googleapis.com/$(cat LATEST_RELEASE_${CHROME_VERSION})/chromedriver_linux64.zip"
                        apt-get install -y unzip
                        unzip chromedriver_linux64.zip
                        mv chromedriver /usr/local/bin/
                        chmod +x /usr/local/bin/chromedriver
                        
                        # Installation de GlassFish 7
                        wget https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                        unzip glassfish-7.0.0.zip -d /opt/
                        chmod -R +x ${GLASSFISH_HOME}/bin
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