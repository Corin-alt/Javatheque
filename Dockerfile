FROM eclipse-temurin:17-jdk

ARG ADMIN_PASSWORD=adminadmin
ARG GLASSFISH_VERSION=7.0.9

ENV GLASSFISH_HOME=/opt/glassfish7 \
   DOMAIN_NAME=domain1 \
   DEPLOY_DIR=/opt/glassfish7/glassfish/domains/domain1/autodeploy \
   DOMAIN_DIR=/opt/glassfish7/glassfish/domains/domain1 \
   PATH=$PATH:/opt/glassfish7/bin

RUN apt-get update && \
   apt-get install -y wget unzip && \
   rm -rf /var/lib/apt/lists/*

RUN groupadd -r glassfish && \
   useradd -r -g glassfish -d $GLASSFISH_HOME glassfish

RUN wget https://download.eclipse.org/ee4j/glassfish/glassfish-${GLASSFISH_VERSION}.zip -O /tmp/glassfish.zip && \
   unzip /tmp/glassfish.zip -d /opt && \
   rm /tmp/glassfish.zip

RUN chown -R glassfish:glassfish $GLASSFISH_HOME && \
   chmod -R 755 $GLASSFISH_HOME/bin

RUN asadmin start-domain ${DOMAIN_NAME} && \
   asadmin stop-domain ${DOMAIN_NAME}

RUN echo "AS_ADMIN_PASSWORD=" > /tmp/pwdfile && \
   echo "AS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" >> /tmp/pwdfile && \
   asadmin start-domain ${DOMAIN_NAME} && \
   asadmin --user admin --passwordfile /tmp/pwdfile change-admin-password && \
   asadmin stop-domain ${DOMAIN_NAME} && \
   rm /tmp/pwdfile && \
   chmod -R 755 $DOMAIN_DIR && \
   chown -R glassfish:glassfish $GLASSFISH_HOME

VOLUME ["$DEPLOY_DIR"]
EXPOSE 8080 4848 8181

USER glassfish
CMD ["asadmin", "start-domain", "--verbose"]