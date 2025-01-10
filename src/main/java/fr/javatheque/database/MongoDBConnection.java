package fr.javatheque.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fr.javatheque.database.codec.LibraryCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import javax.naming.InitialContext;
import javax.naming.NamingException;

public class MongoDBConnection {

    private static MongoClient mongoClient = null;
    private static final String TEST_DATABASE_NAME = "javatheque-locust";
    private static final String PROD_DATABASE_NAME = "javatheque";

    private static final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(new LibraryCodec())
    );

    private static String buildConnectionString() throws NamingException {
        InitialContext context = new InitialContext();
        String url = (String) context.lookup("mongodb/url");
        String user = (String) context.lookup("mongodb/user");
        String password = (String) context.lookup("mongodb/password");

        if (url == null || url.isEmpty()) {
            return "mongodb://root:root@localhost:27017";
        }
        if (url.contains("@")) {
            return url;
        }

        String baseUrl = url.replace("mongodb://", "");
        return String.format("mongodb://%s:%s@%s", user, password, baseUrl);
    }

    private static MongoClient getMongoClient() {
        if (mongoClient == null) {
            try {
                String connectionString = buildConnectionString();
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(connectionString))
                        .codecRegistry(codecRegistry)
                        .build();
                mongoClient = MongoClients.create(settings);
            } catch (NamingException e) {
                System.err.println("Warning: Using default MongoDB connection settings. " + e.getMessage());
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString("mongodb://root:root@localhost:27017"))
                        .codecRegistry(codecRegistry)
                        .build();
                mongoClient = MongoClients.create(settings);
            }
        }
        return mongoClient;
    }

    public static MongoDatabase getDatabase(String dbName) {
        return getMongoClient().getDatabase(dbName);
    }

    public static MongoDatabase getJavathequeDatabase() {
        return getDatabase(PROD_DATABASE_NAME);
    }

    // Nouvelle méthode pour la base de données de test
    public static MongoDatabase getJavathequetLocustDatabase() {
        return getDatabase(TEST_DATABASE_NAME);
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}