package fr.javatheque.database.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import fr.javatheque.database.model.Library;
import fr.javatheque.database.model.User;
import fr.javatheque.util.DatabaseUtils;
import jakarta.ejb.Stateless;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class provides methods to perform CRUD operations on user documents in MongoDB.
 */
@Stateless
public class UserRepository  {

    private static final String USER_ID_KEY = "user_id";
    private static final String LIBRARY_ID_KEY = "library_id";
    private static final String LASTNAME_KEY = "lastname";
    private static final String FIRSTNAME_KEY = "firstname";
    private static final String EMAIL_KEY = "email";
    private static final String PASSWORD_KEY = "password";

    private final MongoCollection<Document> collection;
    private final LibraryRepository libraryRepository;


    public UserRepository() {
        this.libraryRepository = new LibraryRepository();
        this.collection = DatabaseUtils.getDatabase().getCollection("users");
    }

    /**
     * Creates a new user document in the database.
     *
     * @param user The user object to be created.
     * @return The created user object.
     */
    public User createUser(User user) {
        this.libraryRepository.createLibrary(user.getLibrary());
        Document document = createUserDocument(user);
        collection.insertOne(document);
        return user;
    }

    /**
     * Retrieves all users from the database.
     *
     * @return A list of user objects.
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                users.add(documentToUser(document));
            }
        }
        return users;
    }

    /**
     * Retrieves a user by their ID from the database.
     *
     * @param id The ID of the user to retrieve.
     * @return An optional containing the user object, if found.
     */
    public Optional<User> getUserById(String id) {
        Document document = collection.find(Filters.eq(USER_ID_KEY, id)).first();
        return document != null ? Optional.of(documentToUser(document)) : Optional.empty();
    }

    /**
     * Retrieves a user by their email from the database.
     *
     * @param email The email of the user to retrieve.
     * @return An optional containing the user object, if found.
     */
    public Optional<User> getUserByEmail(String email) {
        Document document = collection.find(Filters.eq(EMAIL_KEY, email)).first();
        return document != null ? Optional.of(documentToUser(document)) : Optional.empty();
    }

    /**
     * Updates a user document in the database.
     *
     * @param user The updated user object.
     */
    public void updateUser(User user) {
        Document document = createUserDocument(user);
        collection.replaceOne(Filters.eq(USER_ID_KEY, user.getId()), document);
    }

    /**
     * Deletes a user by their ID from the database.
     *
     * @param id The ID of the user to delete.
     */
    public void deleteUserWithID(String id) {
        collection.deleteOne(Filters.eq(USER_ID_KEY, id));
    }

    /**
     * Deletes a user by their email from the database.
     *
     * @param email The email of the user to delete.
     */
    public void deleteUserWithEmail(String email) {
        collection.deleteOne(Filters.eq(EMAIL_KEY, email));
    }

    /**
     * Creates a MongoDB document representation of a user.
     *
     * @param user the User object to be converted to a document
     * @return the created Document object representing the user
     */
    private Document createUserDocument(User user) {
        return new Document()
                .append(USER_ID_KEY, user.getId())
                .append(LIBRARY_ID_KEY, user.getLibrary().getId())
                .append(LASTNAME_KEY, user.getLastname())
                .append(FIRSTNAME_KEY, user.getFirstname())
                .append(EMAIL_KEY, user.getEmail())
                .append(PASSWORD_KEY, user.getPassword());
    }

    /**
     * Converts a MongoDB document to a User object.
     *
     * @param document the Document object representing the user
     * @return the User object created from the document
     */
    private User documentToUser(Document document) {
        String id = document.getString(USER_ID_KEY);
        Library library = this.libraryRepository.getLibraryById(document.getString(LIBRARY_ID_KEY));
        String lastname = document.getString(LASTNAME_KEY);
        String firstname = document.getString(FIRSTNAME_KEY);
        String email = document.getString(EMAIL_KEY);
        String password = document.getString(PASSWORD_KEY);
        return new User(lastname, firstname, email, password, library, id, false);
    }
}