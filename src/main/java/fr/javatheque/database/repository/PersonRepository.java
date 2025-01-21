package fr.javatheque.database.repository;

import fr.javatheque.database.model.Person;
import jakarta.ejb.Stateless;
import org.bson.Document;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides methods for converting Person objects to Document objects
 * and vice versa for MongoDB operations.
 */
@Stateless
public class PersonRepository {

    private static final String LASTNAME_KEY = "lastname";
    private static final String FIRSTNAME_KEY = "firstname";

    public PersonRepository() {
    }

    /**
     * Converts a Person object to a Document.
     *
     * @param person The Person object to convert.
     * @return The Document representation of the Person.
     */
    public Document toDocument(Person person) {
        return new Document()
                .append(LASTNAME_KEY, person.getLastname())
                .append(FIRSTNAME_KEY, person.getFirstname());
    }

    /**
     * Converts a list of Person objects to a list of Documents.
     *
     * @param persons The list of Person objects to convert.
     * @return The list of Document representations of the Persons.
     */
    public List<Document> toDocuments(List<Person> persons) {
        return persons.stream()
                .map(this::toDocument)
                .collect(Collectors.toList());
    }

    /**
     * Converts a Document to a Person object.
     *
     * @param document The Document to convert.
     * @return The Person object represented by the Document.
     */
    public Person toPerson(Document document) {
        String lastname = document.getString(LASTNAME_KEY);
        String firstname = document.getString(FIRSTNAME_KEY);
        return new Person(lastname, firstname);
    }

    /**
     * Converts a list of Documents to a list of Person objects.
     *
     * @param documents The list of Documents to convert.
     * @return The list of Person objects represented by the Documents.
     */
    public List<Person> toPersons(List<Document> documents) {
        return documents.stream()
                .map(this::toPerson)
                .collect(Collectors.toList());
    }
}