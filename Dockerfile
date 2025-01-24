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
    apt-get install -y wget unzip && \
    rm -rf /var/lib/apt/lists/*

# Install Chrome and ChromeDriver
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get update \
    && apt-get install -y ./google-chrome-stable_current_amd64.deb \
    && LATEST_DRIVER_VERSION=$(wget -qO- "https://chromedriver.storage.googleapis.com/LATEST_RELEASE") \
    && wget -N "https://chromedriver.storage.googleapis.com/$LATEST_DRIVER_VERSION/chromedriver_linux64.zip" \
    && unzip chromedriver_linux64.zip \
    && mv chromedriver /usr/local/bin/ \
    && chmod +x /usr/local/bin/chromedriver \
    && rm google-chrome-stable_current_amd64.deb chromedriver_linux64.zip \
    && rm -rf /var/lib/apt/lists/*

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
    rm /tmp/pwdfile && \
    chmod -R 755 $DOMAIN_DIR && \
    chown -R glassfish:glassfish $GLASSFISH_HOME

# Create volume for auto-deployment
# This is where the WAR file will be mounted from the host
VOLUME ["$DEPLOY_DIR"]

# Expose required ports:
# 8080: HTTP
# 4848: Admin Console
# 8181: HTTPS
EXPOSE 8080 4848 8181

# Switch to glassfish user for security
USER glassfish

# Start GlassFish in verbose mode
CMD ["asadmin", "start-domain", "--verbose"]