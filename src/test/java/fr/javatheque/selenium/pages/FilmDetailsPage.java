package fr.javatheque.selenium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class FilmDetailsPage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(id = "rate")
    private WebElement rateInput;

    @FindBy(id = "opinion")
    private WebElement opinionInput;

    @FindBy(xpath = "//button[@type='submit']")
    private WebElement submitButton;

    @FindBy(id = "lang")
    private WebElement languageSelect;

    @FindBy(id = "support")
    private WebElement supportSelect;

    @FindBy(css = "h1 a")
    private WebElement titleLink;

    public FilmDetailsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void updateFilmDetails(String rate, String opinion) {
        wait.until(ExpectedConditions.visibilityOf(rateInput)).clear();
        rateInput.sendKeys(rate);

        wait.until(ExpectedConditions.visibilityOf(opinionInput)).clear();
        opinionInput.sendKeys(opinion);

        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
    }

    public void updateLanguageAndSupport(String language, String support) {
        Select langSelect = new Select(languageSelect);
        langSelect.selectByValue(language);

        Select suppSelect = new Select(supportSelect);
        suppSelect.selectByValue(support);
    }

    public String getCurrentRate() {
        return wait.until(ExpectedConditions.visibilityOf(rateInput)).getAttribute("value");
    }

    public String getCurrentOpinion() {
        return wait.until(ExpectedConditions.visibilityOf(opinionInput)).getText();
    }

    public void returnToLibrary() {
        wait.until(ExpectedConditions.elementToBeClickable(titleLink)).click();
    }
}