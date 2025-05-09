#!/bin/sh

# Vérifier et corriger les permissions du répertoire d'autodéploiement
echo "Vérification des permissions du répertoire d'autodéploiement..."
if [ ! -d "$DEPLOY_DIR" ]; then
    echo "Création du répertoire $DEPLOY_DIR"
    mkdir -p $DEPLOY_DIR
fi

# Tenter de modifier les permissions
chmod -R 777 $DEPLOY_DIR 2>/dev/null || echo "Avertissement: Impossible de modifier les permissions de $DEPLOY_DIR"
echo "État actuel du répertoire d'autodéploiement:"
ls -la $DEPLOY_DIR

# Si Nginx est installé et nécessaire, le démarrer en arrière-plan
if command -v nginx >/dev/null 2>&1; then
    echo "Démarrage de Nginx en arrière-plan..."
    nginx &
fi

# Démarrer GlassFish au premier plan
echo "Démarrage de GlassFish au premier plan..."
exec asadmin start-domain --verbose ${DOMAIN_NAME}