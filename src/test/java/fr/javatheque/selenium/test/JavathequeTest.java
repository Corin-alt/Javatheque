package fr.javatheque.selenium.test;

import fr.javatheque.selenium.config.WebDriverConfig;
import fr.javatheque.selenium.pages.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JavathequeTest {
    private WebDriver driver;
    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080/javatheque");
    private static final String email = "test" + "-" + System.currentTimeMillis() + "@example.com";
    private static final String password = "TestPassword123";
    private static final String firstname = "John";
    private static final String lastname = "Doe";

    // Pages
    private LoginPage loginPage;
    private RegisterPage registerPage;
    private LibraryPage libraryPage;
    private SearchFilmPage searchFilmPage;
    private FilmSearchResultsPage searchResultsPage;

    @BeforeAll
    void setup() {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        driver = WebDriverConfig.createChromeDriver(headless);

        loginPage = new LoginPage(driver);
        registerPage = new RegisterPage(driver);
        libraryPage = new LibraryPage(driver);
        searchFilmPage = new SearchFilmPage(driver);
        searchResultsPage = new FilmSearchResultsPage(driver);
    }

    @AfterAll
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void waitBriefly() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @Order(1)
    void testLoginWithNonExistentAccount() {
        loginPage.navigateTo(BASE_URL);
        loginPage.login(email, password);
        waitBriefly();
        assertNotNull(driver.getCurrentUrl());
        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Should stay on login page when using non-existent account");
    }

    @Test
    @Order(2)
    void testCreateAccount() {
        registerPage.navigateTo(BASE_URL);
        registerPage.register("Doe", "John", email, password);
        waitBriefly();
        assertNotNull(driver.getCurrentUrl());
        assertTrue(driver.getCurrentUrl().contains("/register"),
                "Should be redirected after successful registration");
    }

    @Test
    @Order(3)
    void testLogin() {
        loginPage.navigateTo(BASE_URL);
        loginPage.login(email, password);
        waitBriefly();
        assertNotNull(driver.getCurrentUrl());
        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Should be redirected after successful login");
    }

    @Test
    @Order(4)
    void testAddFilm() {
        loginPage.navigateTo(BASE_URL);
        loginPage.login(email, password);

        libraryPage.navigateTo(BASE_URL);
        int initialCount = libraryPage.getFilmCount();

        libraryPage.clickSearchFilm();
        searchFilmPage.searchFilm("Avatar", "en-US", "DVD");
        searchResultsPage.addFirstFilm();

        libraryPage.navigateTo(BASE_URL);
        assertTrue(libraryPage.getFilmCount() > initialCount,
                "Film count should increase after adding a new film");
    }

    @Test
    @Order(5)
    void testSearchFilm() {
        loginPage.navigateTo(BASE_URL);
        loginPage.login(email, password);

        libraryPage.navigateTo(BASE_URL);
        libraryPage.searchFilm("Avatar");
        assertTrue(libraryPage.getFilmCount() > 0,
                "Should find the previously added film");
    }

    @Test
    @Order(6)
    void testSearchNonExistentFilm() {
        loginPage.navigateTo(BASE_URL);
        loginPage.login(email, password);

        libraryPage.navigateTo(BASE_URL);
        libraryPage.searchFilm("NonExistentFilm123xyz");
        assertEquals(0, libraryPage.getFilmCount(),
                "Should find no films when searching for non-existent film");
    }

    @Test
    @Order(7)
    void testSearchAvatarFilm() {
        loginPage.navigateTo(BASE_URL);
        loginPage.login(email, password);

        libraryPage.navigateTo(BASE_URL);
        libraryPage.searchFilm("Avatar");
        assertEquals(1, libraryPage.getFilmCount(),
                "Should find exactly one Avatar film in the library");
    }

    @Test
    @Order(8)
    void testLogout() {
        loginPage.navigateTo(BASE_URL);
        loginPage.login(email, password);

        libraryPage.navigateTo(BASE_URL);
        assertTrue(libraryPage.isLoggedIn());

        libraryPage.logout();
        assertFalse(libraryPage.isLoggedIn(),
                "User should be logged out after clicking logout");
    }
}