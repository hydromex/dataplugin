package mx.uabc.mxl.iing.azul.dataplugin.datastore;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCursor;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Configuration;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Reader;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.Collections;

public class MongoDAO {

    private static final MongoClient MONGO;

    static {
        Configuration conf = Reader.getConfiguration();
        MongoCredential cred = MongoCredential.createCredential(conf.getMongoUsername(), conf.getMongoAuthDb(),
                conf.getMongoPassword().toCharArray());

        MONGO = new MongoClient(new ServerAddress(conf.getMongoHost(), conf.getMongoPort()),
                Collections.singletonList(cred));
    }

    public static void insert(String db, String collection, Document ... data) {
        MONGO.getDatabase(db).getCollection(collection).insertMany(Arrays.asList(data));
    }

    public static MongoCursor<Document> find(String db, String collection, Bson query) {
        return MONGO.getDatabase(db).getCollection(collection).find(query).iterator();
    }

    public static void update(String db, String collection, Document query, Document update) {
        MONGO.getDatabase(db).getCollection(collection).updateMany(query, update);
    }
}