package fr.javatheque.database.model;

import fr.javatheque.util.PasswordUtil;

import java.util.*;

/**
 * A class representing a user.
 */
public class User {
    private final String id;

    private String lastname;
    private String firstname;
    private String email;
    private String password;
    private Library library;

    /**
     * Constructs a new User object with the specified properties.
     *
     * @param lastname               the user's last name
     * @param firstname              the user's first name
     * @param email                  the user's email address
     * @param password               the user's password
     * @param library                the user's associated library
     * @param id                     the user's unique identifier
     * @param needToEncryptPassword  flag indicating whether the password needs to be encrypted
     */
    public User(String lastname, String firstname, String email, String password, Library library, String id,
                boolean needToEncryptPassword) {
        this.lastname = lastname;
        this.firstname = firstname;
        this.email = email;
        this.password = needToEncryptPassword ? PasswordUtil.encryptPassword(password) : password;
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.library = library != null ? library : new Library(getId(), new ArrayList<>());
    }

    /**
     * Constructs a new User object with the specified properties.
     *
     * @param lastname   the user's last name
     * @param firstname  the user's first name
     * @param email      the user's email address
     * @param password   the user's password
     */
    public User(String lastname, String firstname, String email, String password) {
        this(lastname, firstname, email, password, null, null, false);
    }

    /**
     * Constructs a new User object with the specified properties.
     *
     * @param lastname   the user's last name
     * @param firstname  the user's first name
     * @param email      the user's email address
     * @param password   the user's password
     * @param id         the user's unique identifier
     */
    public User(String lastname, String firstname, String email, String password, String id) {
        this(lastname, firstname, email, password, null, id, false);
    }

    /**
     * Constructs a new User object with the specified properties.
     *
     * @param lastname               the user's last name
     * @param firstname              the user's first name
     * @param email                  the user's email address
     * @param password               the user's password
     * @param needToEncryptPassword  flag indicating whether the password needs to be encrypted
     */
    public User(String lastname, String firstname, String email, String password, boolean needToEncryptPassword) {
        this(lastname, firstname, email, password, null, null, needToEncryptPassword);
    }

    /**
     * Constructs a new User object with the specified properties.
     *
     * @param lastname               the user's last name
     * @param firstname              the user's first name
     * @param email                  the user's email address
     * @param password               the user's password
     * @param library                the user's associated library
     * @param needToEncryptPassword  flag indicating whether the password needs to be encrypted
     */
    public User(String lastname, String firstname, String email, String password, Library library, boolean needToEncryptPassword) {
        this(lastname, firstname, email, password, library, null, needToEncryptPassword);
    }

    /**
     * Constructs a new User object with the specified properties.
     *
     * @param lastname               the user's last name
     * @param firstname              the user's first name
     * @param email                  the user's email address
     * @param password               the user's password
     * @param id                     the user's unique identifier
     * @param needToEncryptPassword  flag indicating whether the password needs to be encrypted
     */
    public User(String lastname, String firstname, String email, String password, String id, boolean needToEncryptPassword) {
        this(lastname, firstname, email, password, null, id, needToEncryptPassword);
    }

    /**
     * Gets the last name of the user.
     *
     * @return The last name of the user.
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * Sets the last name of the user.
     *
     * @param lastname The last name of the user.
     */
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    /**
     * Gets the first name of the user.
     *
     * @return The first name of the user.
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * Sets the first name of the user.
     *
     * @param firstname The first name of the user.
     */
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    /**
     * Gets the email of the user.
     *
     * @return The email of the user.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email of the user.
     *
     * @param email The email of the user.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the password of the user.
     *
     * @return The password of the user.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password of the user.
     *
     * @param password The password of the user.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the library associated with the user.
     *
     * @return The library associated with the user.
     */
    public Library getLibrary() {
        return library;
    }

    /**
     * Sets the library associated with the user.
     *
     * @param library The library associated with the user.
     */
    public void setLibrary(Library library) {
        this.library = library;
    }

    /**
     * Gets the ID of the user.
     *
     * @return The ID of the user.
     */
    public String getId() {
        return id;
    }
}