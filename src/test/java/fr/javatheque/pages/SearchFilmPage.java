package fr.javatheque.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class SearchFilmPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(id = "title")
    private WebElement titleInput;

    @FindBy(id = "lang")
    private WebElement languageSelect;

    @FindBy(id = "support")
    private WebElement supportSelect;

    @FindBy(id = "show_existent_film")
    private WebElement searchForm;

    @FindBy(css = "h1 a")
    private WebElement homeLink;

    public SearchFilmPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/film/search");
    }

    public void searchFilm(String title, String language, String support) {
        wait.until(ExpectedConditions.visibilityOf(titleInput)).sendKeys(title);

        Select langSelect = new Select(languageSelect);
        langSelect.selectByValue(language);

        Select suppSelect = new Select(supportSelect);
        suppSelect.selectByValue(support);

        wait.until(ExpectedConditions.elementToBeClickable(searchForm)).submit();
    }

    public void returnToHome() {
        wait.until(ExpectedConditions.elementToBeClickable(homeLink)).click();
    }

    public String getCurrentLanguage() {
        Select select = new Select(languageSelect);
        return select.getFirstSelectedOption().getAttribute("value");
    }

    public String getCurrentSupport() {
        Select select = new Select(supportSelect);
        return select.getFirstSelectedOption().getAttribute("value");
    }
}