pipeline {
    // Pas d'agent global - chaque étape définit son propre environnement
    agent none

    // Déclenché automatiquement sur push GitHub
    triggers {
        githubPush()
    }

    // Variables d'environnement globales pour la configuration
    environment {
        // Configuration de l'application
        APP_NAME = 'javatheque'
        DOCKER_IMAGE = 'javatheque-env'
        DOCKER_TAG = 'latest'
        DOCKER_REGISTRY = 'ghcr.io'

        DB_VOLUME= 'mongodb_volume'
       
        // Credentials sécurisés
        GLASSFISH_ADMIN_PASSWORD = credentials('glassfish-admin-password')
        GITHUB_OWNER = 'corin-alt'
        GITHUB_TOKEN = credentials('github-token')
        DEPLOY_PPROD_SERVER = credentials('deploy-pprod-server')
        DEPLOY_PROD_SERVER = credentials('deploy-prod-server')
        SUDO_PASSWORD = credentials('sudo-password')
        
        // Chemins de déploiement
        APP_PATH = 'javatheque'
        APP_DEPLOY_PATH= './target'
    }

    stages {
        // Étape 1: Compilation Maven du projet
        stage('Maven Build') {
            agent {
                // Utilisation de Maven avec JDK 17 dans un conteneur
                docker {
                    image 'maven:3.9.9-eclipse-temurin-17'
                    args '-v $HOME/.m2:/root/.m2 -u root'
                }
            }
            steps {
                // Compilation et packaging sans exécuter les tests
                sh 'mvn clean package -DskipTests'
                // Archivage du WAR généré
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }

        // Étape 2: Exécution des tests unitaires
        stage('Unit Tests') {
            agent {
                // Même environnement Maven que l'étape précédente
                docker {
                    image 'maven:3.9.9-eclipse-temurin-17'
                    args '-v $HOME/.m2:/root/.m2 -u root'
                }
            }
            steps {
                // Exécution uniquement des tests unitaires
                sh 'mvn clean test -Dtest=**/*UnitTest'
            }
        }

        // Étape 3: Construction et publication de l'image Docker
        stage('Build Docker Image') {
            agent {
                // Utilisation de Docker-in-Docker pour build l'image
                docker {
                    image 'docker:dind'
                    args '--privileged -v /var/run/docker.sock:/var/run/docker.sock -u root'
                }
            }
            // Exécuté uniquement si build réussi et sur branches dev/main
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    anyOf {
                        expression { env.GIT_BRANCH == 'origin/dev' }
                        expression { env.GIT_BRANCH == 'origin/main' }
                    }
                }
            }
            steps {
                // Build et push de l'image avec les credentials GitHub
                withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                    script {
                        sh '''
                        mkdir -p /root/.docker
                        # Build de l'image avec mot de passe admin GlassFish
                        docker build --build-arg ADMIN_PASSWORD=${GLASSFISH_ADMIN_PASSWORD} \
                                -t ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG} .
                        # Connexion et push vers GitHub Container Registry
                        echo $GITHUB_TOKEN | docker login ${DOCKER_REGISTRY} -u ${GITHUB_OWNER} --password-stdin
                        docker push ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker logout ${DOCKER_REGISTRY}
                        '''
                    }
                }
            }
        }

        // Étape 4: Déploiement en environnement de pré-production
        stage('Deploy to Pre-production') {
            agent {
                // Utilisation d'Ubuntu pour le déploiement
                docker {
                    image 'ubuntu:latest'
                    args '-u root'
                }
            }
            // Uniquement sur la branche dev après succès des étapes précédentes
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { env.GIT_BRANCH == 'origin/dev' }
                }
            }
            steps {
                // Installation des outils nécessaires
                sh '''
                    apt-get update && apt-get install -y openssh-client rsync
                    mkdir -p target
                    chmod -R 777  target
                '''
                // Récupération des artifacts du build
                copyArtifacts filter: 'target/*.war', fingerprintArtifacts: true, projectName: '${JOB_NAME}', selector: specific('${BUILD_NUMBER}')
                sh 'chmod -R 777  target'
                // Déploiement via SSH
                sshagent(credentials: ['jenkins-ssh-private-key']) {
                    sh '''
                    # Configuration SSH
                    mkdir -p ~/.ssh
                    chmod 700 ~/.ssh
                    TARGET_IP=$(echo $DEPLOY_PPROD_SERVER | cut -d'@' -f2)
                    ssh-keyscan -H $TARGET_IP >> ~/.ssh/known_hosts
                    chmod 644 ~/.ssh/known_hosts

                    # Synchronisation des fichiers
                    ssh ${DEPLOY_PPROD_SERVER} "rm -rf ${APP_PATH}/target/.autodeploystatus || true"
                    rsync -av --delete --exclude=${DB_VOLUME} ./ ${DEPLOY_PPROD_SERVER}:${APP_PATH}/
                    scp target/${APP_NAME}.war ${DEPLOY_PPROD_SERVER}:${APP_PATH}/target

                    # Configuration environnement
                    ssh ${DEPLOY_PPROD_SERVER} "printf 'DOCKER_REGISTRY=%s\\nGITHUB_OWNER=%s\\nDOCKER_IMAGE=%s\\nDOCKER_TAG=%s\\nAPP_DEPLOY_PATH=%s' '${DOCKER_REGISTRY}' '${GITHUB_OWNER}' '${DOCKER_IMAGE}' '${DOCKER_TAG}' '${APP_DEPLOY_PATH}' > ${APP_PATH}/.env"

                    # Déploiement Docker
                    ssh ${DEPLOY_PPROD_SERVER} "cd ${APP_PATH} && \
                        echo ${GITHUB_TOKEN} | docker login ghcr.io -u ${GITHUB_OWNER} --password-stdin || exit 1 && \
                        docker pull ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG} || true && \
                        docker compose down || echo 'Warning: docker compose down failed but continuing' && \
                        docker compose up -d || exit 1 && \
                        docker logout ghcr.io"
                    '''
                }
            }
        }

        // Étape 5: Déploiement en production
        stage('Deploy to Production') {
            agent {
                // Même configuration que pré-production
                docker {
                    image 'ubuntu:latest'
                    args '-u root'
                }
            }
            // Uniquement sur la branche main après succès des étapes précédentes
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
            }
            steps {
                // Installation des outils nécessaires
                sh '''
                    apt-get update && apt-get install -y openssh-client rsync
                    mkdir -p target
                    chmod -R 777  target
                '''
                // Récupération des artifacts
                copyArtifacts filter: 'target/*.war', fingerprintArtifacts: true, projectName: '${JOB_NAME}', selector: specific('${BUILD_NUMBER}')
                sh 'chmod -R 777  target'
                // Déploiement via SSH
                sshagent(credentials: ['jenkins-ssh-private-key']) {
                    sh '''
                    # Configuration SSH
                    mkdir -p ~/.ssh
                    chmod 700 ~/.ssh
                    TARGET_IP=$(echo $DEPLOY_PROD_SERVER | cut -d'@' -f2)
                    ssh-keyscan -H $TARGET_IP >> ~/.ssh/known_hosts
                    chmod 644 ~/.ssh/known_hosts

                    # Synchronisation des fichiers
                    ssh ${DEPLOY_PPROD_SERVER} "rm -rf ${APP_PATH}/target/.autodeploystatus || true"
                    rsync -av --delete ./ ${DEPLOY_PROD_SERVER}:${APP_PATH}/
                    scp target/${APP_NAME}.war ${DEPLOY_PROD_SERVER}:${APP_PATH}/target

                    # Configuration environnement
                    ssh ${DEPLOY_PROD_SERVER} "printf 'DOCKER_REGISTRY=%s\\nGITHUB_OWNER=%s\\nDOCKER_IMAGE=%s\\nDOCKER_TAG=%s\\nAPP_DEPLOY_PATH=%s' '${DOCKER_REGISTRY}' '${GITHUB_OWNER}' '${DOCKER_IMAGE}' '${DOCKER_TAG}' '${APP_DEPLOY_PATH}' > ${APP_PATH}/.env"

                    # Déploiement Docker
                    ssh ${DEPLOY_PROD_SERVER} "cd ${APP_PATH} && \
                        echo ${GITHUB_TOKEN} | docker login ghcr.io -u ${GITHUB_OWNER} --password-stdin || exit 1 && \
                        docker pull ${DOCKER_REGISTRY}/${GITHUB_OWNER}/${DOCKER_IMAGE}:${DOCKER_TAG} || true && \
                        docker compose down || echo 'Warning: docker compose down failed but continuing' && \
                        docker compose up -d || exit 1 && \
                        docker logout ghcr.io"
                    '''
                }
            }
        }
    }

    // Actions post-pipeline pour nettoyage et notifications
    post {
        // Nettoyage systématique de l'espace de travail
        always {
            script {
                node('built-in') {
                    cleanWs()
                }
            }
        }
        // Notification en cas d'échec
        failure {
            script {
                node('built-in') {
                    echo 'Pipeline failed!'
                }
            }
        }
        // Notification en cas de succès
        success {
            script {
                node('built-in') {
                    echo 'Pipeline completed successfully!'
                }
            }
        }
    }
}
