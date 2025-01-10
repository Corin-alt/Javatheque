package fr.javatheque.util;

import com.mongodb.client.MongoDatabase;
import fr.javatheque.database.MongoDBConnection;
import org.bson.Document;

public class DatabaseUtils {
    private static final ThreadLocal<Boolean> isTestEnvironment = new ThreadLocal<>();

    public static void setTestEnvironment(boolean isTest) {
        isTestEnvironment.set(isTest);
    }

    public static void clearEnvironment() {
        isTestEnvironment.remove();
    }

    public static MongoDatabase getDatabase() {
        Boolean isTest = isTestEnvironment.get();
        return (isTest != null && isTest)
                ? MongoDBConnection.getJavathequetLocustDatabase()
                : MongoDBConnection.getJavathequeDatabase();
    }


    public static void clearDatabase() {
        MongoDatabase database = getDatabase();
        if (database == null) return;

        String[] collections = {"users", "libraries", "films"};

        for (String collectionName : collections) {
            database.getCollection(collectionName).deleteMany(new Document());
        }
    }
}