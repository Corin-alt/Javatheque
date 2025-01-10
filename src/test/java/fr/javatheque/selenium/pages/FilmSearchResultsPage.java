package fr.javatheque.selenium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;

public class FilmSearchResultsPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(css = "table tbody tr")
    private List<WebElement> searchResults;

    @FindBy(css = "button[type='submit']")
    private List<WebElement> addButtons;

    @FindBy(xpath = "//button[contains(text(), 'Next Page')]")
    private WebElement nextPageButton;

    @FindBy(xpath = "//button[contains(text(), 'Previous Page')]")
    private WebElement previousPageButton;

    @FindBy(css = ".film-card img")
    private List<WebElement> filmPosters;

    @FindBy(css = "p:contains('No results found.')")
    private WebElement noResultsMessage;

    public FilmSearchResultsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public int getResultCount() {
        return searchResults.size();
    }

    public void addFilmByIndex(int index) {
        if (index < addButtons.size()) {
            wait.until(ExpectedConditions.elementToBeClickable(addButtons.get(index))).click();
        }
    }

    public void addFirstFilm() {
        addFilmByIndex(0);
    }

    public boolean hasNextPage() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(nextPageButton)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasPreviousPage() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(previousPageButton)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void goToNextPage() {
        if (hasNextPage()) {
            wait.until(ExpectedConditions.elementToBeClickable(nextPageButton)).click();
        }
    }

    public void goToPreviousPage() {
        if (hasPreviousPage()) {
            wait.until(ExpectedConditions.elementToBeClickable(previousPageButton)).click();
        }
    }

    public boolean arePostersLoaded() {
        return !filmPosters.isEmpty() &&
                filmPosters.stream()
                        .allMatch(poster -> poster.getAttribute("complete") != null);
    }

    public boolean hasNoResults() {
        try {
            return wait.until(ExpectedConditions.visibilityOf(noResultsMessage)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}