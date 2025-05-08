#!/bin/bash

# Configuration
GITHUB_URL="https://github.com/corin-alt/javatheque.git"
LOCAL_REPO="/git/javatheque"
JENKINS_URL="http://localhost:8080"
JENKINS_ADMIN_USER="admin"
JENKINS_API_TOKEN="your-api-token"

# Create a temporary directory
TEMP_DIR=$(mktemp -d)
echo "Created temporary directory: $TEMP_DIR"

# Clone the GitHub repository
echo "Cloning $GITHUB_URL..."
git clone $GITHUB_URL $TEMP_DIR
if [ $? -ne 0 ]; then
    echo "Error: Failed to clone GitHub repository"
    rm -rf $TEMP_DIR
    exit 1
fi

# Navigate to the cloned repository
cd $TEMP_DIR

# Verify we're on main branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "Current branch: $CURRENT_BRANCH"
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "Error: Not on main branch"
    cd /tmp
    rm -rf $TEMP_DIR
    exit 1
fi

# Add the local repository as a remote
echo "Adding local repository as remote..."
git remote add local file://$LOCAL_REPO
if [ $? -ne 0 ]; then
    echo "Error: Failed to add local repository as remote"
    cd /tmp
    rm -rf $TEMP_DIR
    exit 1
fi

# Push to the local repository
echo "Pushing to local repository..."
git push local main
if [ $? -ne 0 ]; then
    echo "Error: Failed to push to local repository"
    cd /tmp
    rm -rf $TEMP_DIR
    exit 1
fi

# Clean up
echo "Cleaning up..."
cd /tmp
rm -rf $TEMP_DIR

echo "Synchronization completed successfully!"

# Trigger Jenkins build with CSRF protection (crumb)
echo "Triggering Jenkins build..."

# Get Jenkins crumb for CSRF protection
echo "Getting Jenkins crumb..."
CRUMB=$(curl -s "$JENKINS_URL/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)" -u "$JENKINS_ADMIN_USER:$JENKINS_API_TOKEN")

if [ -z "$CRUMB" ]; then
    echo "Warning: Could not get Jenkins crumb. Trying without it..."
    curl -X POST "$JENKINS_URL/job/Javatheque/build?token=$JENKINS_API_TOKEN" -u "$JENKINS_ADMIN_USER:$JENKINS_API_TOKEN"
else
    echo "Triggering build with crumb..."
    curl -X POST "$JENKINS_URL/job/Javatheque/build?token=$JENKINS_API_TOKEN" -H "$CRUMB" -u "$JENKINS_ADMIN_USER:$JENKINS_API_TOKEN"
fi

# Wait for build to start
echo "Waiting for build to start..."
sleep 5

# Check build status
echo "Checking build status..."
BUILD_NUMBER=$(curl -s "$JENKINS_URL/job/Javatheque/lastBuild/api/json" -u "$JENKINS_ADMIN_USER:$JENKINS_API_TOKEN" | grep -o '"number":[0-9]*' | cut -d':' -f2)
echo "Build number: $BUILD_NUMBER"

# Monitor build progress
while true; do
    STATUS=$(curl -s "$JENKINS_URL/job/Javatheque/$BUILD_NUMBER/api/json" -u "$JENKINS_ADMIN_USER:$JENKINS_API_TOKEN" | grep -o '"result":"[^"]*"' | cut -d'"' -f4)
    
    if [ "$STATUS" = "SUCCESS" ]; then
        echo "Build completed successfully!"
        break
    elif [ "$STATUS" = "FAILURE" ]; then
        echo "Build failed!"
        exit 1
    elif [ "$STATUS" = "ABORTED" ]; then
        echo "Build was aborted!"
        exit 1
    fi
    
    echo "Build in progress... Current status: $STATUS"
    sleep 10
done 