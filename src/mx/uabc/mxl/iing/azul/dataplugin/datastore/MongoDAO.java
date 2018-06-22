package mx.uabc.mxl.iing.azul.dataplugin.datastore;
/*
    Copyright (C) 2018  Jesús Donaldo Osornio Hernández
    Copyright (C) 2018  Luis Alejandro Herrera León
    Copyright (C) 2018  Gabriel Alejandro López Morteo

    This file is part of DataPlugin.

    DataPlugin is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataPlugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataPlugin.  If not, see <http://www.gnu.org/licenses/>.
*/

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Configuration;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Reader;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class serves as a Data Access Object to interact with the MongoDB persistence layer. It has methods to
 * insert and update documents, as well as to query collections
 *
 * @author jdosornio
 * @version %I%
 */
public class MongoDAO {

    private static final MongoClient MONGO;

    static {
        Configuration conf = Reader.getConfiguration();
        MongoCredential cred = MongoCredential.createCredential(conf.getMongoUsername(), conf.getMongoAuthDb(),
                conf.getMongoPassword().toCharArray());

        MONGO = new MongoClient(new ServerAddress(conf.getMongoHost(), conf.getMongoPort()),
                Collections.singletonList(cred));
    }

    /**
     * This method inserts a given list of documents into the specified collection and database
     *
     * @param db the MongoDB database name
     * @param collection the MongoDB collection name
     * @param data the document(s) to insert into MongoDB
     */
    public static void insert(String db, String collection, Document ... data) {
        MONGO.getDatabase(db).getCollection(collection).insertMany(Arrays.asList(data));
    }

    /**
     * This method is used to find documents in the specified database and collection, given the
     * required filters and projections
     *
     * @param db the MongoDB database name
     * @param collection the MongoDB collection name
     * @param query a {@link Bson} object with the filters to apply to the query
     * @param proj a {@link Bson} object with the projections to apply to the query
     *
     * @return a list of documents with the results of the query
     */
    public static List<Document> find(String db, String collection, Bson query, Bson proj) {
        return MONGO.getDatabase(db).getCollection(collection).find(query).projection(proj)
                .into(new ArrayList<>());
    }

    /**
     * This method updates the given collection and database with the specified query and data
     *
     * @param db the MongoDB database name
     * @param collection the MongoDB collection name
     * @param query a {@link Bson} object with the filters to apply to the query
     * @param update a {@link Bson} object with the data to be replaced in the resulting documents
     */
    public static void update(String db, String collection, Document query, Document update) {
        MONGO.getDatabase(db).getCollection(collection).updateMany(query, update);
    }
}