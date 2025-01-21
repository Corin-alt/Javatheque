pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                // Ajoutez vos commandes de build ici
                sh 'echo "Building..."'
            }
        }
        
        stage('Test') {
            steps {
                // Ajoutez vos commandes de test ici
                sh 'echo "Testing..."'
            }
        }
        
        stage('Deploy') {
            steps {
                // Ajoutez vos commandes de déploiement ici
                sh 'echo "Deploying..."'
            }
        }
    }
}