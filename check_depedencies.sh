#!/bin/bash

check_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "❌ $1 n'est pas installé"
        return 1
    fi
    return 0
}

check_java_version() {
    if ! java -version 2>&1 | grep -q "version \"17"; then
        echo "❌ Java 17 n'est pas installé"
        return 1
    fi
    return 0
}

check_python_packages() {
    if ! python3 -c "import selenium" 2>/dev/null; then
        echo "❌ Le package Python Selenium n'est pas installé"
        return 1
    fi
    return 0
}

# Vérification des dépendances
failed=0

echo "Vérification des dépendances requises..."

check_command "docker" || failed=1
check_command "java" && check_java_version || failed=1
check_command "python3" || failed=1
check_command "pip" || failed=1
check_command "mvn" || failed=1
check_command "google-chrome" || failed=1
check_command "chromedriver" || failed=1
check_command "locust" || failed=1
check_python_packages || failed=1

if [ $failed -eq 1 ]; then
    echo "⛔ Certaines dépendances requises ne sont pas installées"
    exit 1
else
    echo "✅ Toutes les dépendances sont installées"
    exit 0
fi
