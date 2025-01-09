#!/bin/sh
# Démarrer GlassFish en arrière-plan
asadmin start-domain ${DOMAIN_NAME} &

# Démarrer Nginx au premier plan
nginx -g 'daemon off;'