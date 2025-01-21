package fr.javatheque.database.repository;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import fr.javatheque.database.model.Film;
import fr.javatheque.database.model.Person;
import fr.javatheque.util.DatabaseUtils;
import jakarta.ejb.Stateless;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class provides methods for CRUD operations on Film objects in MongoDB.
 */
@Stateless
public class FilmRepository {
    private static final String FILM_ID_KEY = "film_id";
    private static final String LIBRARY_ID_KEY = "library_id";
    private static final String POSTER_KEY = "poster";
    private static final String LANG_KEY = "lang";
    private static final String SUPPORT_KEY = "support";
    private static final String TITLE_KEY = "title";
    private static final String DESCRIPTION_KEY = "description";
    private static final String RELEASE_DATE_KEY = "releaseDate";
    private static final String YEAR_KEY = "year";
    private static final String RATE_KEY = "rate";
    private static final String OPINION_KEY = "opinion";
    private static final String DIRECTOR_KEY = "director";
    private static final String ACTORS_KEY = "actors";

    private final MongoCollection<Document> collection;
    private final PersonRepository personRepository;

    public FilmRepository() {
        this.collection = DatabaseUtils.getDatabase().getCollection("films");
        this.personRepository = new PersonRepository();
    }

    /**
     * Creates a new film in the database.
     *
     * @param film The film to create.
     * @return The created film.
     */
    public Film createFilm(Film film) {
        Document document = filmToDocument(film);
        collection.insertOne(document);
        return film;
    }

    /**
     * Retrieves all films from the database.
     *
     * @return A list of films.
     */
    public List<Film> getAllFilms() {
        List<Film> films = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            cursor.forEachRemaining(doc -> films.add(documentToFilm(doc)));
        }
        return films;
    }

    /**
     * Retrieves films by library ID from the database.
     *
     * @param libraryId The ID of the library containing the films.
     * @return A list of films in the specified library.
     */
    public List<Film> getFilmsByLibraryId(String libraryId) {
        List<Film> films = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find(Filters.eq(LIBRARY_ID_KEY, libraryId)).iterator()) {
            cursor.forEachRemaining(doc -> films.add(documentToFilm(doc)));
        }
        return films;
    }

    /**
     * Retrieves a film by its ID from the database.
     *
     * @param id The ID of the film.
     * @return An optional containing the film, if found; otherwise, empty.
     */
    public Optional<Film> getFilmById(int id) {
        Document document = collection.find(Filters.eq(FILM_ID_KEY, id)).first();
        return document != null ? Optional.of(documentToFilm(document)) : Optional.empty();
    }

    /**
     * Updates a film in the database.
     *
     * @param film The updated film.
     */
    public void updateFilm(Film film) {
        Document document = filmToDocument(film);
        collection.replaceOne(Filters.eq(FILM_ID_KEY, film.getId()), document);
    }

    /**
     * Deletes a film from the database by its ID.
     *
     * @param id The ID of the film to delete.
     */
    public void deleteFilm(int id) {
        collection.deleteOne(Filters.eq(FILM_ID_KEY, id));
    }

    /**
     * Converts a Film object to a MongoDB document.
     *
     * @param film the Film object to be converted to a document
     * @return the created Document object representing the film
     */
    private Document filmToDocument(Film film) {
        return new Document(FILM_ID_KEY, film.getId())
                .append(LIBRARY_ID_KEY, film.getLibraryId())
                .append(POSTER_KEY, film.getPoster())
                .append(LANG_KEY, film.getLang())
                .append(SUPPORT_KEY, film.getSupport())
                .append(TITLE_KEY, film.getTitle())
                .append(DESCRIPTION_KEY, film.getDescription())
                .append(RELEASE_DATE_KEY, film.getReleaseDate())
                .append(YEAR_KEY, film.getYear())
                .append(RATE_KEY, film.getRate())
                .append(OPINION_KEY, film.getOpinion())
                .append(DIRECTOR_KEY, personRepository.toDocument(film.getDirector()))
                .append(ACTORS_KEY, personRepository.toDocuments(film.getActors()));
    }

    /**
     * Converts a MongoDB document to a Film object.
     *
     * @param document the Document object representing the film
     * @return the Film object created from the document
     */
    private Film documentToFilm(Document document) {
        int id = document.getInteger(FILM_ID_KEY);
        String libraryId = document.getString(LIBRARY_ID_KEY);
        String poster = document.getString(POSTER_KEY);
        String lang = document.getString(LANG_KEY);
        String support = document.getString(SUPPORT_KEY);
        String title = document.getString(TITLE_KEY);
        String description = document.getString(DESCRIPTION_KEY);
        String releaseDate = document.getString(RELEASE_DATE_KEY);
        String year = document.getString(YEAR_KEY);
        float rate = document.getDouble(RATE_KEY).floatValue();
        String opinion = document.getString(OPINION_KEY);
        Person director = personRepository.toPerson((Document) document.get(DIRECTOR_KEY));
        List<Person> actors = personRepository.toPersons(document.getList(ACTORS_KEY, Document.class));
        return new Film(id, libraryId, poster, lang, support, title, description, releaseDate, year, rate, opinion, director, actors);
    }
}