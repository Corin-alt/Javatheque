package fr.javatheque.database.model;

import java.util.List;

/**
 * Film represents a film entity with various attributes such as ID, poster, language, support, title, description, release date,
 * year, rate, opinion, director, and a list of actors.
 */
public class Film {
    private final int id;
    private String libraryId;
    private String poster;
    private String lang;
    private String support;
    private String title;
    private String description;
    private String releaseDate;
    private String year;
    private float rate;
    private String opinion;
    private Person director;
    private List<Person> actors;

    public Film(int id, String libraryId, String poster, String lang, String support, String title, String description,
                String releaseDate, String year, float rate, String opinion, Person director, List<Person> actors) {
        this.id = id;
        this.libraryId = libraryId;
        this.poster = poster;
        this.lang = lang;
        this.support = support;
        this.title = title;
        this.description = description;
        this.releaseDate = releaseDate;
        this.year = year;
        this.rate = rate;
        this.opinion = opinion;
        this.director = director;
        this.actors = actors;
    }

    public int getId() {
        return id;
    }

    public String getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public float getRate() {
        return rate;
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public Person getDirector() {
        return director;
    }

    public void setDirector(Person director) {
        this.director = director;
    }

    public List<Person> getActors() {
        return actors;
    }

    public void setActors(List<Person> actors) {
        this.actors = actors;
    }
}
