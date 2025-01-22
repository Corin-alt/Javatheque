pipeline {
    agent none
        
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
        stage('Setup Tests Environment') {
            agent {
                docker {
                    image 'selenium/standalone-chrome'
                    args '-u root'
                }
            }
            steps {
                script {
                    sh '''
                        set -e
                        apt-get update
                        apt-get install -y wget unzip rsync
                        
                        # Vérifier l'espace disque disponible
                        echo "Checking disk space..."
                        df -h
                        
                        # Install Java 17
                        echo "Installing Java 17..."
                        apt-get install -y apt-transport-https ca-certificates wget
                        mkdir -p /etc/apt/keyrings
                        wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | tee /etc/apt/keyrings/adoptium.asc
                        echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
                        apt-get update
                        apt-get install -y temurin-17-jdk
                        
                        echo "Verifying Java installation..."
                        java -version
                        
                        # GlassFish installation
                        echo "Starting GlassFish installation..."
                        wget -v https://download.eclipse.org/ee4j/glassfish/glassfish-7.0.0.zip
                        
                        echo "Creating GlassFish directories..."
                        rm -rf /opt/glassfish7
                        mkdir -p /opt/glassfish7
                        
                        echo "Unzipping GlassFish..."
                        unzip -o glassfish-7.0.0.zip -d /opt/

                        # Copy GlassFish to workspace for stashing using rsync
                        echo "Copying GlassFish to workspace..."
                        mkdir -p ${WORKSPACE}/glassfish7
                        rsync -av --exclude='*.log' --exclude='*.jar.lock' /opt/glassfish7/ ${WORKSPACE}/glassfish7/
                        
                        # Vérifier que la copie s'est bien passée
                        echo "Verifying GlassFish copy..."
                        ls -la ${WORKSPACE}/glassfish7/
                        du -sh ${WORKSPACE}/glassfish7/
                        
                        # S'assurer que les permissions sont correctes
                        chmod -R 644 ${WORKSPACE}/glassfish7/
                        find ${WORKSPACE}/glassfish7/ -type d -exec chmod 755 {} \;
                    '''
                    
                    // Stash GlassFish from workspace with allowEmpty
                    stash includes: 'glassfish7/**', name: 'glassfish', allowEmpty: false
                    
                    // Vérifier que le stash a bien été créé
                    echo "Verifying stash contents..."
                    sh 'find ${WORKSPACE}/glassfish7 -type f | wc -l'
                }
            }
        }

        stage('Build & Deploy') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    args '-u root'
                }
            }
            steps {
                script {
                    // Récupérer GlassFish du stage précédent
                    unstash 'glassfish'
                    
                    // Copier GlassFish vers /opt
                    sh '''
                        cp -r glassfish7 /opt/
                        chmod -R 755 ${GLASSFISH_HOME}
                        chmod -R +x ${GLASSFISH_HOME}/bin
                        
                        echo "Testing GlassFish..."
                        ${GLASSFISH_HOME}/bin/asadmin version
                    '''
                    
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
                    
                    // Build avec Maven
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
    }
    
    post {
        always {
            node('built-in') {
                cleanWs()
            }
        }
        failure {
            node('built-in') {
                echo 'Pipeline failed!'
            }
        }
        success {
            node('built-in') {
                echo 'Pipeline completed successfully!'
            }
        }
    }
}