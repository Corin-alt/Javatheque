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
                        apt-get install -y software-properties-common
                        add-apt-repository -y ppa:openjdk-r/ppa
                        apt-get update
                        apt-get install -y openjdk-17-jdk
                        
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
                        
                        # GlassFish installation with verbose output
                        echo "Starting GlassFish installation..."
                        
                        echo "Downloading GlassFish..."
                        wget -v https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                        
                        echo "Listing current /opt directory..."
                        ls -la /opt/
                        
                        echo "Unzipping GlassFish..."
                        unzip -v glassfish-7.0.0.zip -d /opt/
                        
                        echo "Verifying GlassFish installation..."
                        ls -la /opt/glassfish7
                        ls -la ${GLASSFISH_HOME}/bin
                        
                        echo "Setting permissions..."
                        chmod -R +x ${GLASSFISH_HOME}/bin
                        
                        echo "Verifying final permissions..."
                        ls -la ${GLASSFISH_HOME}/bin/asadmin
                        
                        echo "Checking GlassFish version..."
                        ${GLASSFISH_HOME}/bin/asadmin version || echo "Failed to get version"
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