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

/**
 * This class provides methods to establish a connection to MongoDB
 * and retrieve database instances.
 */
public class MongoDBConnection {

    private static MongoClient mongoClient = null;

    // CodecRegistry for custom codecs
    private static final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(new LibraryCodec())
    );

    /**
     * Builds the connection string from GlassFish resources.
     *
     * @return The MongoDB connection string.
     * @throws NamingException if the resources cannot be found
     */
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

    /**
     * Retrieves the MongoClient instance, creating it if it doesn't exist.
     *
     * @return The MongoClient instance.
     */
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

    /**
     * Retrieves the MongoDatabase instance for the specified database name.
     *
     * @param dbName The name of the MongoDB database.
     * @return The MongoDatabase instance.
     */
    public static MongoDatabase getDatabase(String dbName) {
        return getMongoClient().getDatabase(dbName);
    }

    /**
     * Retrieves the MongoDatabase instance for the 'javatheque' database.
     *
     * @return The MongoDatabase instance for the 'javatheque' database.
     */
    public static MongoDatabase getJavathequeDatabase() {
        return getDatabase("javatheque");
    }

    /**
     * Closes the MongoDB connection.
     * Should be called when the application shuts down.
     */
    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}