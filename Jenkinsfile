pipeline {
    options {
        timeout(time: 1, unit: 'HOURS')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
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
        
        // Credentials
        DEPLOY_SERVER = credentials('deploy-server')
        DB_URL = credentials('db_url')
        DB_USER = credentials('db_user')
        DB_PASSWORD = credentials('db_password')
        
        // Paths
        APP_CODE_PATH = '/apps/java/src'
        APP_DEPLOY_PATH = '/apps/java/deploy'
        GLASSFISH_HOME = '/opt/glassfish7'
        
        // Flags and Options
        DOCKERFILE_CHANGED = 'false'
        CHROME_OPTIONS = '--headless --no-sandbox --disable-dev-shm-usage'
    }
    
    tools {
        maven 'Maven'
        dockerTool 'Docker'
        jdk 'JDK17'
    }
    
    stages {
        stage('Validate Environment') {
            steps {
                script {
                    // Verify credentials are available
                    if (!env.DB_URL || !env.DB_USER || !env.DB_PASSWORD) {
                        error "Database credentials not properly configured"
                    }
                }
            }
        }

        stage('Setup Environment') {
            steps {
                script {
                    sh '''
                        set -e  # Exit on any error
                        
                        # Update package lists with retry
                        for i in {1..3}; do
                            if apt-get update; then
                                break
                            fi
                            echo "Attempt $i failed. Retrying after 10s..."
                            sleep 10
                        done
                        
                        # Install base dependencies
                        apt-get install -y wget gnupg curl unzip
                        
                        # Setup Chrome repository and install Chrome
                        wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
                        echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google.list
                        apt-get update
                        apt-get install -y google-chrome-stable
                        
                        # Install ChromeDriver
                        CHROME_VERSION=$(google-chrome --version | awk '{ print $3 }' | cut -d'.' -f1)
                        wget -N "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_VERSION}"
                        wget -N "https://chromedriver.storage.googleapis.com/$(cat LATEST_RELEASE_${CHROME_VERSION})/chromedriver_linux64.zip"
                        unzip -o chromedriver_linux64.zip
                        mv chromedriver /usr/local/bin/
                        chmod +x /usr/local/bin/chromedriver
                        
                        # Install GlassFish 7
                        if [ ! -d "${GLASSFISH_HOME}" ]; then
                            wget https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                            unzip glassfish-7.0.0.zip -d /opt/
                            chmod -R +x ${GLASSFISH_HOME}/bin
                        fi
                    '''
                    
                    // Verify installations
                    sh '''
                        echo "Verifying installations..."
                        google-chrome --version
                        chromedriver --version
                        ${GLASSFISH_HOME}/bin/asadmin --version
                    '''
                }
            }
        }
        
        stage('Build and Test') {
            steps {
                sh 'mvn clean package'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
    
    post {
        always {
            script {
                try {
                    sh '''
                        # Attempt to stop GlassFish domain gracefully
                        if [ -f "${GLASSFISH_HOME}/bin/asadmin" ]; then
                            ${GLASSFISH_HOME}/bin/asadmin stop-domain domain1 || true
                        fi
                    '''
                } catch (Exception e) {
                    echo "Warning: Failed to stop GlassFish domain: ${e.getMessage()}"
                } finally {
                    // Clean workspace
                    cleanWs()
                }
            }
        }
        
        success {
            script {
                echo 'Pipeline successfully executed!'
                // You can add notifications here (email, Slack, etc.)
            }
        }
        
        failure {
            script {
                echo 'Pipeline failed!'
                // You can add failure notifications here
            }
        }
    }
}