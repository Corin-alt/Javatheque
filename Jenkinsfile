pipeline {
    agent none
    
    options {
        copyArtifactPermission('*')
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
        DB_HOST = 'localhost'
        DB_PORT = '27017'
        DB_USER = credentials('db_user')
        DB_PASSWORD = credentials('db_password')
        DB_NAME = credentials('db_name')
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
                        find ${WORKSPACE}/glassfish7/ -type d -exec chmod 755 {} +
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
                    // Configuration GlassFish...
                    unstash 'glassfish'
                    
                    sh '''
                        mkdir -p ${GLASSFISH_HOME}
                        cp -r glassfish7/. ${GLASSFISH_HOME}/
                        chmod -R 755 ${GLASSFISH_HOME}
                        chmod -R +x ${GLASSFISH_HOME}/bin
                    '''
                    
                    // Build et création du WAR
                    sh '''
                        mvn clean package -DskipTests
                        
                        echo "Vérification du WAR..."
                        WAR_FILE="target/javatheque.war"
                        if [ ! -f "$WAR_FILE" ]; then
                            echo "ERROR: Le fichier WAR n'a pas été créé"
                            exit 1
                        fi
                        
                        # Vérifier la taille du fichier
                        WAR_SIZE=$(stat -f%z "$WAR_FILE" 2>/dev/null || stat -c%s "$WAR_FILE")
                        if [ "$WAR_SIZE" -eq 0 ]; then
                            echo "ERROR: Le fichier WAR est vide"
                            exit 1
                        fi
                        
                        echo "WAR créé avec succès (taille: $WAR_SIZE bytes)"
                        
                        # S'assurer que le fichier est accessible
                        chmod 644 "$WAR_FILE"
                    '''
                    
                    // Stash le WAR pour une utilisation ultérieure
                    stash includes: 'target/*.war', name: 'war-file', allowEmpty: false
                    
                    // Archiver aussi pour la traçabilité
                    archiveArtifacts artifacts: 'target/*.war', 
                                fingerprint: true,
                                onlyIfSuccessful: true
                }
            }
        }

        stage('Check retrieving WAR file') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    args '-u root'
                }
            }
            steps {
                script {
                    // Création des répertoires avec permissions
                    sh '''
                        mkdir -p ${WORKSPACE}/target
                        chmod -R 755 ${WORKSPACE}/target
                    '''
                    
                    // Récupérer le WAR via unstash au lieu de copyArtifacts
                    unstash 'war-file'
                    
                    // Configuration GlassFish
                    unstash 'glassfish'

                    sh '''
                        mkdir -p ${GLASSFISH_HOME}
                        cp -r glassfish7/. ${GLASSFISH_HOME}/
                        chmod -R 755 ${GLASSFISH_HOME}
                        chmod -R +x ${GLASSFISH_HOME}/bin
                        
                        # Vérification du WAR
                        echo "Vérification du WAR..."
                        WAR_PATH=$(find ${WORKSPACE}/target -name "*.war")
                        if [ -z "$WAR_PATH" ]; then
                            echo "ERROR: WAR non trouvé après unstash"
                            exit 1
                        fi
                        
                        WAR_SIZE=$(stat -f%z "$WAR_PATH" 2>/dev/null || stat -c%s "$WAR_PATH")
                        echo "WAR trouvé: $WAR_PATH (taille: $WAR_SIZE bytes)"
                        
                        if [ "$WAR_SIZE" -eq 0 ]; then
                            echo "ERROR: Le fichier WAR est vide"
                            exit 1
                        fi
                        
                        ls -l $WAR_PATH
                    '''
                }
            }
        }
    }
    
    post {
        always {
            node('built-in') {
                script {
                    sh '''
                        if [ -f "${GLASSFISH_HOME}/bin/asadmin" ]; then
                            echo "Stopping GlassFish domain..."
                            ${GLASSFISH_HOME}/bin/asadmin stop-domain domain1 || true
                        fi
                    '''
                }
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