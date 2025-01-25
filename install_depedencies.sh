#!/bin/bash

command_exists() {
    command -v "$1" >/dev/null 2>&1
}

print_message() {
    echo "==> $1"
}

install_docker() {
    if ! command_exists docker; then
        print_message "Installing Docker..."
        sudo apt-get update
        sudo apt-get install -y ca-certificates curl
        sudo install -m 0755 -d /etc/apt/keyrings
        sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
        sudo chmod a+r /etc/apt/keyrings/docker.asc
        
        echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
        $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
        
        sudo apt-get update
        sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    else
        print_message "Docker is already installed"
    fi
}

install_java() {
    if ! command_exists java || ! java -version 2>&1 | grep -q "version \"17"; then
        print_message "Installing Java 17..."
        sudo apt-get update
        sudo apt-get install -y openjdk-17-jdk
    else
        print_message "Java 17 is already installed"
    fi
}

install_python() {
    if ! command_exists python3; then
        print_message "Installing Python..."
        sudo apt-get update
        sudo apt-get install -y python3-full
    else
        print_message "Python is already installed"
    fi

    if ! command_exists pip; then
        print_message "Installing pip for Python..."
        sudo apt-get install -y python3-pip
    else
        print_message "pip is already installed"
    fi
}

install_maven() {
    if ! command_exists mvn; then
        print_message "Installing Maven..."
        sudo apt-get update
        sudo apt-get install -y maven
    else
        print_message "Maven is already installed"
    fi
}

install_chrome() {
    if ! command_exists google-chrome; then
        print_message "Installing Chrome and ChromeDriver..."
        wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
        sudo apt-get update
        sudo apt-get install -y ./google-chrome-stable_current_amd64.deb
        
        LATEST_DRIVER_VERSION=$(wget -qO- "https://chromedriver.storage.googleapis.com/LATEST_RELEASE")
        wget -N "https://chromedriver.storage.googleapis.com/$LATEST_DRIVER_VERSION/chromedriver_linux64.zip"
        unzip chromedriver_linux64.zip
        sudo mv chromedriver /usr/local/bin/
        sudo chmod +x /usr/local/bin/chromedriver
        rm google-chrome-stable_current_amd64.deb chromedriver_linux64.zip
        sudo rm -rf /var/lib/apt/lists/*
    else
        print_message "Chrome is already installed"
    fi
}

install_selenium() {
    print_message "Installing/Updating Selenium..."
    sudo apt install python3-selenium
}

install_locust() {
    if ! command_exists locust; then
        print_message "Installing Locust..."
        sudo apt-get update
        sudo apt install python3-locust
    else
        print_message "Locust is already installed"
    fi
}

print_message "Starting dependencies installation..."

install_docker
install_java
install_python
install_maven
install_chrome
install_selenium
install_locust

print_message "Installation complete!"
