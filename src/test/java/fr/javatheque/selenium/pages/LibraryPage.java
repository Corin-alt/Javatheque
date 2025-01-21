package fr.javatheque.selenium.pages;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class LibraryPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(id = "search-film")
    private WebElement searchFilmButton;

    @FindBy(id = "search-bar")
    private WebElement searchInput;

    @FindBy(id = "search-button")
    private WebElement searchButton;

    @FindBy(className = "film-card")
    private List<WebElement> filmCards;

    @FindBy(className = "success-message")
    private WebElement successMessage;

    @FindBy(id = "logout")
    private WebElement logoutButton;

    public LibraryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/library?search=all");
    }

    public void clickSearchFilm() {
        wait.until(ExpectedConditions.elementToBeClickable(searchFilmButton)).click();
    }

    public void searchFilm(String title) {
        wait.until(ExpectedConditions.visibilityOf(searchInput)).clear();
        searchInput.sendKeys(title);
        wait.until(ExpectedConditions.elementToBeClickable(searchButton)).click();
    }

    public int getFilmCount() {
        return filmCards.size();
    }

    public String getSuccessMessage() {
        return wait.until(ExpectedConditions.visibilityOf(successMessage)).getText();
    }

    public void logout() {
        wait.until(ExpectedConditions.elementToBeClickable(logoutButton)).click();
    }

    public boolean isLoggedIn() {
        try {
            wait.until(ExpectedConditions.visibilityOf(logoutButton));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}