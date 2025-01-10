package fr.javatheque.database.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import fr.javatheque.database.model.Library;
import fr.javatheque.util.DatabaseUtils;
import jakarta.ejb.Stateless;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides methods for CRUD operations on Library objects in MongoDB.
 */
@Stateless
public class LibraryRepository{

    private static final String LIBRARY_ID_KEY = "library_id";
    private static final String OWNER_ID_KEY = "owner_id";

    private final MongoCollection<Document> collection;
    private final FilmRepository filmRepository;


    public LibraryRepository() {
        this.collection =  DatabaseUtils.getDatabase().getCollection("libraries");
        this.filmRepository = new FilmRepository();
    }

    /**
     * Creates a new library in the database.
     *
     * @param library The library to create.
     * @return The created library.
     */
    public Library createLibrary(Library library) {
        library.getFilms().forEach(filmRepository::createFilm);
        Document document = new Document(LIBRARY_ID_KEY, library.getId())
                .append(OWNER_ID_KEY, library.getOwnerId());
        collection.insertOne(document);
        return library;
    }

    /**
     * Retrieves all libraries from the database.
     *
     * @return A list of libraries.
     */
    public List<Library> getAllLibraries() {
        List<Library> libraries = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            cursor.forEachRemaining(doc -> libraries.add(documentToLibrary(doc)));
        }
        return libraries;
    }

    /**
     * Retrieves a library by its owner's ID from the database.
     *
     * @param ownerId The ID of the library's owner.
     * @return The library, if found; otherwise, null.
     */
    public Library getLibraryByOwnerId(String ownerId) {
        Document document = collection.find(Filters.eq(OWNER_ID_KEY, ownerId)).first();
        return document != null ? documentToLibrary(document) : null;
    }

    /**
     * Retrieves a library by its ID from the database.
     *
     * @param libraryId The ID of the library.
     * @return The library, if found; otherwise, null.
     */
    public Library getLibraryById(String libraryId) {
        Document document = collection.find(Filters.eq(LIBRARY_ID_KEY, libraryId)).first();
        return document != null ? documentToLibrary(document) : null;
    }

    /**
     * Updates a library in the database.
     *
     * @param library The updated library.
     */
    public void updateLibrary(Library library) {
        library.getFilms().forEach(filmRepository::createFilm);
        Document document = new Document(LIBRARY_ID_KEY, library.getId())
                .append(OWNER_ID_KEY, library.getOwnerId());
        collection.replaceOne(Filters.eq(OWNER_ID_KEY, library.getOwnerId()), document);
    }

    /**
     * Deletes a library by its owner's ID from the database.
     *
     * @param ownerId The ID of the library's owner.
     */
    public void deleteLibraryByOwnerId(String ownerId) {
        collection.deleteOne(Filters.eq(OWNER_ID_KEY, ownerId));
    }

    /**
     * Converts a Document to a Library object.
     *
     * @param document The Document to convert.
     * @return The Library object.
     */
    Library documentToLibrary(Document document) {
        String libraryId = document.getString(LIBRARY_ID_KEY);
        String ownerId = document.getString(OWNER_ID_KEY);
        return new Library(libraryId, ownerId, filmRepository.getFilmsByLibraryId(libraryId));
    }
}