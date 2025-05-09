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
import java.util.logging.Logger;
import java.util.logging.Level;

public class MongoDBConnection {
    private static final Logger LOGGER = Logger.getLogger(MongoDBConnection.class.getName());
    private static MongoClient mongoClient = null;
    private static final String TEST_DATABASE_NAME = "javatheque-locust";
    private static final String PROD_DATABASE_NAME = "javatheque";

    private static final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(new LibraryCodec())
    );

    private static String buildConnectionString() throws NamingException {
        LOGGER.info("Building MongoDB connection string...");
        InitialContext context = new InitialContext();
        String url = (String) context.lookup("mongodb/url");
        String user = (String) context.lookup("mongodb/user");
        String password = (String) context.lookup("mongodb/password");

        LOGGER.info("JNDI lookup results - URL: " + url + ", User: " + user);

        if (url == null || url.isEmpty()) {
            LOGGER.info("Using default MongoDB connection string");
            return "mongodb://root:root@mongodb:27017";
        }
        if (url.contains("@")) {
            LOGGER.info("Using provided MongoDB connection string with credentials");
            return url;
        }

        String baseUrl = url.replace("mongodb://", "");
        String connectionString = String.format("mongodb://%s:%s@%s", user, password, baseUrl);
        LOGGER.info("Built MongoDB connection string: " + connectionString);
        return connectionString;
    }

    private static MongoClient getMongoClient() {
        if (mongoClient == null) {
            try {
                LOGGER.info("Creating new MongoDB client...");
                String connectionString = buildConnectionString();
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(connectionString))
                        .codecRegistry(codecRegistry)
                        .build();
                mongoClient = MongoClients.create(settings);
                LOGGER.info("MongoDB client created successfully");
            } catch (NamingException e) {
                LOGGER.log(Level.WARNING, "Warning: Using default MongoDB connection settings. " + e.getMessage());
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString("mongodb://root:root@mongodb:27017"))
                        .codecRegistry(codecRegistry)
                        .build();
                mongoClient = MongoClients.create(settings);
                LOGGER.info("MongoDB client created with default settings");
            }
        }
        return mongoClient;
    }

    public static MongoDatabase getDatabase(String dbName) {
        LOGGER.info("Getting database: " + dbName);
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
            LOGGER.info("Closing MongoDB client");
            mongoClient.close();
            mongoClient = null;
        }
    }
}