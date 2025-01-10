package fr.javatheque.selenium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class RegisterPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(id = "lastname")
    private WebElement lastnameInput;

    @FindBy(id = "firstname")
    private WebElement firstnameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "register_user")
    private WebElement registerForm;

    public RegisterPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/register");
    }

    public void register(String lastname, String firstname, String email, String password) {
        wait.until(ExpectedConditions.visibilityOf(lastnameInput)).sendKeys(lastname);
        wait.until(ExpectedConditions.visibilityOf(firstnameInput)).sendKeys(firstname);
        wait.until(ExpectedConditions.visibilityOf(emailInput)).sendKeys(email);
        wait.until(ExpectedConditions.visibilityOf(passwordInput)).sendKeys(password);
        wait.until(ExpectedConditions.elementToBeClickable(registerForm)).submit();
    }
}