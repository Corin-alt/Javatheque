#!/bin/sh

# Check and fix autodeploy directory permissions
echo "Checking autodeploy directory permissions..."
if [ ! -d "$DEPLOY_DIR" ]; then
    echo "Creating directory $DEPLOY_DIR"
    mkdir -p $DEPLOY_DIR
fi

# Try to modify permissions (may fail if mounted as a volume)
chmod -R 777 $DEPLOY_DIR 2>/dev/null || echo "Warning: Could not change permissions on $DEPLOY_DIR"
echo "Current state of autodeploy directory:"
ls -la $DEPLOY_DIR

# Start GlassFish in the background
echo "Starting GlassFish..."
asadmin start-domain ${DOMAIN_NAME} &
GLASSFISH_PID=$!

# If Nginx is installed, start it in the foreground
if command -v nginx >/dev/null 2>&1; then
    echo "Starting Nginx..."
    nginx -g 'daemon off;'
else
    echo "Nginx is not installed, running GlassFish in the foreground..."
    # Keep the script running by waiting for GlassFish
    wait $GLASSFISH_PID
fi