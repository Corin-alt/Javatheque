# Base image with Java 17
FROM eclipse-temurin:17-jdk

# Environment variables for customization
ARG ADMIN_PASSWORD=adminadmin
ARG GLASSFISH_VERSION=7.0.9

# GlassFish configuration paths
ENV GLASSFISH_HOME=/opt/glassfish7
ENV DOMAIN_NAME=domain1
ENV DEPLOY_DIR=$GLASSFISH_HOME/glassfish/domains/$DOMAIN_NAME/autodeploy
ENV DOMAIN_DIR=$GLASSFISH_HOME/glassfish/domains/$DOMAIN_NAME
ENV PATH=$PATH:$GLASSFISH_HOME/bin

# Install required tools
RUN apt-get update && \
    apt-get install -y wget unzip curl && \
    rm -rf /var/lib/apt/lists/*

# Create GlassFish user for security
RUN groupadd -r glassfish && \
    useradd -r -g glassfish -d $GLASSFISH_HOME glassfish

# Download and install GlassFish
RUN wget https://download.eclipse.org/ee4j/glassfish/glassfish-${GLASSFISH_VERSION}.zip -O /tmp/glassfish.zip && \
    unzip /tmp/glassfish.zip -d /opt && \
    rm /tmp/glassfish.zip

# Set up permissions for GlassFish directories
RUN chown -R glassfish:glassfish $GLASSFISH_HOME && \
    chmod -R 755 $GLASSFISH_HOME/bin

# Initial domain creation
RUN asadmin start-domain ${DOMAIN_NAME} && \
    asadmin stop-domain ${DOMAIN_NAME}

# Configure admin password
RUN echo "AS_ADMIN_PASSWORD=" > /tmp/pwdfile && \
    echo "AS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" >> /tmp/pwdfile && \
    asadmin start-domain ${DOMAIN_NAME} && \
    asadmin --user admin --passwordfile /tmp/pwdfile change-admin-password && \
    asadmin stop-domain ${DOMAIN_NAME} && \
    rm /tmp/pwdfile

# Make sure autodeploy directory exists and has correct permissions
RUN mkdir -p $DEPLOY_DIR && \
    chmod -R 777 $DEPLOY_DIR && \
    chown -R glassfish:glassfish $GLASSFISH_HOME

# Create bundles directory for OSGi bundles
RUN mkdir -p $DEPLOY_DIR/bundles && \
    chmod -R 777 $DEPLOY_DIR/bundles && \
    chown -R glassfish:glassfish $DEPLOY_DIR/bundles

# Copy the entrypoint script from your project
COPY start.sh /start.sh
RUN chmod +x /start.sh

# Create volume for auto-deployment
VOLUME ["$DEPLOY_DIR"]

# Expose required ports:
# 8080: HTTP
# 4848: Admin Console
# 8181: HTTPS
EXPOSE 8080 4848 8181

# Set working directory
WORKDIR $GLASSFISH_HOME

# Use the entrypoint script
CMD ["/start.sh"]