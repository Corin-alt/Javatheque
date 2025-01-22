pipeline {
    agent none
        
    environment {
        APP_NAME = 'javatheque'
        DOCKER_IMAGE = 'javatheque-env'
        DOCKER_TAG = 'latest'
        DOCKER_REGISTRY = 'ghcr.io'
        GITHUB_OWNER = 'corin-alt'

        // Credentials 
        GITHUB_TOKEN = credentials('github-token')
        DEPLOY_SERVER = credentials('deploy-server')
        DB_USER = credentials('db_user')
        DB_NAME = credentials('db_name')
        DB_PASSWORD = credentials('db_password')
        DB_HOST = 'localhost'
        DB_PORT = '27017'

        // Paths
        APP_CODE_PATH = '/apps/java/src'
        APP_DEPLOY_PATH = '/apps/java/deploy'
        GLASSFISH_HOME = '/opt/glassfish7'

        // Flags and Options
        DOCKERFILE_CHANGED = 'false'
        CHROME_OPTIONS = '--headless --no-sandbox --disable-dev-shm-usage'
    }
        
    stages {
        stage('Build & Test') {
            agent {
                docker {
                    image 'ubuntu:latest'
                    args '-u root'
                }
            }
            steps {
                script {
                    sh '''
                        set -e
                        apt-get update
                        apt-get install -y wget gnupg curl unzip
                        
                        # Chrome et ChromeDriver
                        wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
                        echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google.list
                        apt-get update
                        wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
                        apt-get install -y ./google-chrome-stable_current_amd64.deb
                        
                        LATEST_DRIVER_VERSION=$(wget -qO- "https://chromedriver.storage.googleapis.com/LATEST_RELEASE")
                        wget -N "https://chromedriver.storage.googleapis.com/$LATEST_DRIVER_VERSION/chromedriver_linux64.zip"
                        unzip chromedriver_linux64.zip
                        mv chromedriver /usr/local/bin/
                        chmod +x /usr/local/bin/chromedriver
                        
                        # GlassFish
                        wget https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                        unzip glassfish-7.0.0.zip -d /opt/
                        chmod -R +x ${GLASSFISH_HOME}/bin
                    '''
                }
            }
        }
        stage('Checkout & Build application') {
            steps {
                script {
                    checkout([$class: 'GitSCM', 
                            branches: [[name: '*/main']], 
                            extensions: [[$class: 'CloneOption', depth: 1, noTags: true]]])

                    def dbUrl = "mongodb://${env.DB_USER}:${env.DB_PASSWORD}@${env.DB_HOST}:${env.DB_PORT}/${env.DB_NAME}"
                    
                    sh """
                        ${GLASSFISH_HOME}/bin/asadmin start-domain domain1
                    
                        timeout 60 bash -c 'until ${GLASSFISH_HOME}/bin/asadmin list-domains | grep "domain1 running"; do sleep 2; done'

                        ${GLASSFISH_HOME}/bin/asadmin create-custom-resource \
                            --restype=java.lang.String \
                            --factoryclass=org.glassfish.resources.custom.factory.PrimitivesAndStringFactory \
                            --property value=${dbUrl} \
                            mongodb/url || exit 1
                        
                        ${GLASSFISH_HOME}/bin/asadmin create-custom-resource \
                            --restype=java.lang.String \
                            --factoryclass=org.glassfish.resources.custom.factory.PrimitivesAndStringFactory \
                            --property value=${env.DB_USER} \
                            mongodb/user || exit 1
                            
                        ${GLASSFISH_HOME}/bin/asadmin create-custom-resource \
                            --restype=java.lang.String \
                            --factoryclass=org.glassfish.resources.custom.factory.PrimitivesAndStringFactory \
                            --property value=${env.DB_PASSWORD} \
                            mongodb/password || exit 1
                    """
                    
                    def buildResult = sh(script: 'mvn clean package -DskipTests', returnStatus: true)
                    if (buildResult != 0) {
                        error "Maven build failed with status ${buildResult}"
                    }
                }
            }
        }
    }
    
    post {
        always {
            node('built-in') {
                script {
                    try {
                        sh '''
                            if [ -f "${GLASSFISH_HOME}/bin/asadmin" ]; then
                                ${GLASSFISH_HOME}/bin/asadmin stop-domain domain1 || true
                            fi
                        '''
                    } catch (Exception e) {
                        echo "Warning: Failed to stop GlassFish domain: ${e.getMessage()}"
                    } finally {
                        cleanWs()
                    }
                }
            }
        }
        success {
            node('built-in') {
                echo 'Pipeline successfully executed!'
            }
        }
        failure {
            node('built-in') {
                echo 'Pipeline failed!'
            }
        }
    }
}