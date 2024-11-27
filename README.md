# Javatheque

Javatheque is a project to create an API rest for managing a video library using Java Jakarta.

This project is part of module RT0805 of the Master DAS.
## Authors

- Dupont Corentin
- Morlet Flavien

## Usage

Before you start, you'll need to install **Glassfish**.

If you are on Windows don't forget to launch "**Docker Desktop**".

### Start a domain

```powershell
asadmin start-domain [domaine-name]
```
### Give execution permission to mvnw file
```powershell
cd javatheque/

# If you are on Windows use a Git terminal or WSL to perform these commands
chmod +x mvnw
chmod +x mvnw.cmd
```
### Run MongoDB
```powershell
cd javatheque/

docker-compose up --build
```

### Deploy for the first time

```powershell
.\mvnw clean install

asadmin deploy .\target\javatheque.war
```

### Redeploy

```powershell
.\mvnw clean install

asadmin redeploy --name javatheque .\target\javatheque.war
```

You can access at : http://localhost:8080/javatheque

