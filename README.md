# Javatheque

Javatheque is a project to create an API rest for managing a video library using Java Jakarta.

This project is part of module RT0805 of the Master DAS.
## Authors

- Corentin
- Flavien

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

## Tests

### Unit
```powershell
mvn test clean -Dtest=**/*UnitTest
```

### Selenium
For these tests, the application must be deployed and run !

A chrome browser must be installed on your own machine ! 

```powershell
mvn test  clean -Dtest=**/*SeleniumTest -Dbrowser=chrome -DbaseUrl=http://localhost:8080/javatheque -Dheadless=false
```

### Locust

```powershell
cd src/test/java/fr/javatheque/locust

# Stress test
locust --host=http://localhost:8080/javatheque --users 1000 --spawn-rate 100 --run-time 1m --headless --html=stress_report.html

# Loading test
locust --host=http://localhost:8080/javatheque --users 500 --spawn-rate 10 --run-time 15m --headless --html=load_report.html
```

At the end of test, a web report will be created.

