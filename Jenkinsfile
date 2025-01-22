pipeline {
    agent {
        docker {
            image 'ubuntu:latest'
            args '-u root'
        }
    }
        
    environment {
        APP_NAME = 'javatheque'
        DOCKER_IMAGE = 'javatheque-env'
        DOCKER_TAG = 'latest'
        DOCKER_REGISTRY = 'ghcr.io'
        GITHUB_OWNER = 'corin-alt'
        GLASSFISH_HOME = '/opt/glassfish7'
        DB_HOST = 'localhost'
        DB_PORT = '27017'
    }
        
    stages {
        stage('Build & Test') {
            steps {
                script {
                    sh '''
                        set -e
                        apt-get update
                        apt-get install -y wget gnupg curl unzip
                        
                        # Install Java 17
                        echo "Installing Java 17..."
                        apt-get install -y wget
                        apt-get update && apt-get install -y apt-transport-https ca-certificates
                        mkdir -p /etc/apt/keyrings
                        wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | tee /etc/apt/keyrings/adoptium.asc
                        echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
                        apt-get update
                        apt-get install -y temurin-17-jdk
                        
                        echo "Verifying Java installation..."
                        java -version
                        
                        # Chrome and ChromeDriver installation
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
                        
                        # GlassFish installation with verbose output and additional checks
                        echo "Starting GlassFish installation..."
                        
                        echo "Downloading GlassFish..."
                        wget -v https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                        
                        echo "Cleaning any existing GlassFish installation..."
                        rm -rf /opt/glassfish7
                        
                        echo "Creating GlassFish directory..."
                        mkdir -p /opt/glassfish7
                        
                        echo "Unzipping GlassFish with full permissions..."
                        unzip -o glassfish-7.0.0.zip -d /opt/
                        
                        echo "Setting correct ownership and permissions..."
                        chown -R root:root ${GLASSFISH_HOME}
                        chmod -R 755 ${GLASSFISH_HOME}
                        chmod -R +x ${GLASSFISH_HOME}/bin
                        
                        echo "Verifying installation structure..."
                        if [ ! -f "${GLASSFISH_HOME}/bin/asadmin" ]; then
                            echo "asadmin not found in expected location"
                            find /opt -name asadmin
                            exit 1
                        fi
                        
                        echo "Verifying GlassFish bin contents:"
                        ls -la ${GLASSFISH_HOME}/bin
                        
                        echo "Verifying executable permissions:"
                        ls -la ${GLASSFISH_HOME}/bin/asadmin
                        
                        echo "Checking Java environment for GlassFish:"
                        echo "JAVA_HOME=$JAVA_HOME"
                        echo "PATH=$PATH"
                        
                        echo "Testing GlassFish asadmin:"
                        ${GLASSFISH_HOME}/bin/asadmin version || {
                            echo "Failed to get GlassFish version"
                            echo "Checking if asadmin is executable:"
                            file ${GLASSFISH_HOME}/bin/asadmin
                            echo "Checking asadmin contents:"
                            head -n 5 ${GLASSFISH_HOME}/bin/asadmin
                            exit 1
                        }
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

                    withCredentials([
                        usernamePassword(credentialsId: 'db_user', usernameVariable: 'DB_USER', passwordVariable: 'DB_PASSWORD'),
                        string(credentialsId: 'db_name', variable: 'DB_NAME')
                    ]) {
                        // Construct MongoDB URL using single quotes to prevent interpolation
                        def dbUrl = 'mongodb://' + DB_USER + ':' + DB_PASSWORD + '@' + env.DB_HOST + ':' + env.DB_PORT + '/' + DB_NAME
                        
                        sh """#!/bin/bash -e
                            ${GLASSFISH_HOME}/bin/asadmin start-domain domain1
                        
                            timeout 60 bash -c 'until ${GLASSFISH_HOME}/bin/asadmin list-domains | grep "domain1 running"; do sleep 2; done'

                            ${GLASSFISH_HOME}/bin/asadmin create-custom-resource \\
                                --restype=java.lang.String \\
                                --factoryclass=org.glassfish.resources.custom.factory.PrimitivesAndStringFactory \\
                                --property value='\${dbUrl}' \\
                                mongodb/url || true
                            
                            ${GLASSFISH_HOME}/bin/asadmin create-custom-resource \\
                                --restype=java.lang.String \\
                                --factoryclass=org.glassfish.resources.custom.factory.PrimitivesAndStringFactory \\
                                --property value='\${DB_USER}' \\
                                mongodb/user || true
                                
                            ${GLASSFISH_HOME}/bin/asadmin create-custom-resource \\
                                --restype=java.lang.String \\
                                --factoryclass=org.glassfish.resources.custom.factory.PrimitivesAndStringFactory \\
                                --property value='\${DB_PASSWORD}' \\
                                mongodb/password || true
                        """
                    }
                    
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