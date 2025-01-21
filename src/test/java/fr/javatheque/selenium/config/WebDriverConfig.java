package fr.javatheque.selenium.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class WebDriverConfig {
    public static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            options.setBinary("/usr/bin/google-chrome");
        } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            options.setBinary("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        }

        if (headless) {
            options.addArguments("--headless");
        }
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        if (System.getenv("JENKINS_HOME") != null) {
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--ignore-certificate-errors");
        }

        return new ChromeDriver(options);
    }

    public static void quitDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}