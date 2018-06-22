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

import static com.mongodb.client.model.Projections.*;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Reader;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * This class is tasked with the manipulation of the Dataplugin metadata catalogs, such as the plugin
 * and the database ones. Its main functions are registering and obtaining metadata
 *
 * @author jdosornio
 * @version %I%
 */
public class CatalogManager {

    private static final String PLUGIN_CATALOG = "plugins";
    private static final String PLUGIN_NAME_K = "name";
    private static final String PLUGIN_VERSION_K = "version";
    private static final String PLUGIN_ID_K = "_id";
    private static final String PLUGIN_VENDOR_K = "vendor";

    private static final String DATABASE_CATALOG = "databases";
    private static final String DATABASE_NAME_K = "name";
    private static final String DATABASE_PLUGIN_ID_K = "pluginId";
    private static final String DATABASE_CREATED_AT_K = "created_at";
    private static final String DATABASE_UPDATED_AT_K = "updated_at";
    private static final String DATABASE_CREATED_BY_K = "created_by";

    private static final String GTE_OP = "$gte";
    private static final String LTE_OP = "$lte";

    /**
     * This method registers the plugin metadata in the plugin metadata catalog
     *
     * @param plugin the {@link Plugin} object to register
     */
    public static void registerPlugin(Plugin plugin) {
        //See if the same name and version of the plugin already exists, if so don't create it again...
        if (findPlugin(plugin.getName(), plugin.getVersion()) == null) {
            Document pluginMetadata = new Document(plugin.getMetadata().asMap());
            MongoDAO.insert(Reader.getConfiguration().getCatalogDatabase(), PLUGIN_CATALOG, pluginMetadata);
            MessageMediator.sendMessage("Plugin: [" + plugin.getName() + "] registered in catalog");
        }
    }

    /**
     * This method registers the database metadata in the database/data-set metadata catalog, associating
     * the name and version of the plugin used to load the database data into Dataplugin
     *
     * @param pluginName the plugin name
     * @param pluginVersion the plugin version
     * @param database a {@link Document} object representing the database metadata
     */
    public static void registerDatabase(String pluginName, String pluginVersion, Document database) {
        //Must validate db_schema document in some way... At least for the required fields...
        //Almost the same logic as plugin, just that, in this case, the field updated_at is updated
        Document db = findDatabase(database.getString(DATABASE_NAME_K));
        if (db != null) {
            //Later change to allow modification of the schema (always insert a new schema, just getting the created_at val
            //Update updated_at field
            MongoDAO.update(Reader.getConfiguration().getCatalogDatabase(), DATABASE_CATALOG,
                    new Document(DATABASE_NAME_K, db.getString(DATABASE_NAME_K)),
                    new Document("$set", new Document(DATABASE_UPDATED_AT_K, Instant.now().toEpochMilli())));
            MessageMediator.sendMessage("Updated database: [" + db.getString(DATABASE_NAME_K) + "]");

        } else {
            //If the database does not exists, add a created_at field with the current timestamp.
            //Before inserting, create a created_by field and add the id of the plugin...
            long currTime = Instant.now().toEpochMilli();
            Document pluginMeta = findPlugin(pluginName, pluginVersion);

            database.put(DATABASE_PLUGIN_ID_K, pluginMeta.get(PLUGIN_ID_K));
            database.put(DATABASE_CREATED_AT_K, currTime);
            database.put(DATABASE_UPDATED_AT_K, currTime);
            //Add also created_by key...
            database.put(DATABASE_CREATED_BY_K, pluginMeta.getString(PLUGIN_VENDOR_K));

            MongoDAO.insert(Reader.getConfiguration().getCatalogDatabase(), DATABASE_CATALOG, database);
            MessageMediator.sendMessage("Registered database: [" + database.getString(DATABASE_NAME_K) +
                    "] using the plugin: [" + pluginName + "] with version: [" + pluginVersion + "]");
        }

    }

    /**
     * Obtains the metadata of an existing plugin given its name and version
     *
     * @param name the plugin name to search for
     * @param version the plugin version to search for
     *
     * @return a document object representing the plugin metadata stored in the plugin catalog
     */
    public static Document findPlugin(String name, String version) {
        List<Document> res = MongoDAO.find(Reader.getConfiguration().getCatalogDatabase(), PLUGIN_CATALOG,
                new Document(PLUGIN_NAME_K, name).append(PLUGIN_VERSION_K, version), null);

        return (!res.isEmpty()) ? res.get(0) : null;
    }

    /**
     * Obtains the metadata of an existing database/data-set given its name
     *
     * @param name the database/data-set name to search for
     *
     * @return a document object representing the database/data-set metadata stored in the database/data-set catalog
     */
    public static Document findDatabase(String name) {
        return findDatabase(name, null);
    }

    /**
     * Local method to find a database/data-set metadata document with the specified projections
     *
     * @param name the database/data-set name to search for
     * @param proj a {@link Bson} object containing the projections to apply on the query result
     *
     * @return a {@link Document} object with the database/data-set metadata, projected with the specified fields
     */
    private static Document findDatabase(String name, Bson proj) {
        List<Document> res = MongoDAO.find(Reader.getConfiguration().getCatalogDatabase(), DATABASE_CATALOG,
                new Document(DATABASE_NAME_K, name), proj);

        return (!res.isEmpty()) ? res.get(0) : null;
    }

    /**
     * Get a list of database/data-set metadata documents stored in the database/data-set catalog, optionally
     * filtering the records which where last modified between the given start and end dates.
     *
     * @param fromDate start date from which to get all the later records, may be null
     * @param toDate end date until which to get all the earlier records, may be null
     *
     * @return a list of document objects with the metadata records queried
     */
    public static List<Document> getDatabases(LocalDate fromDate, LocalDate toDate) {
        Document query = new Document();
        Document filter = new Document();

        //Filter if necessary
        if (fromDate != null) {
            filter.put(GTE_OP, fromDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        if(toDate != null) {
            filter.put(LTE_OP, toDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        if(!filter.isEmpty()) {
            query.put(DATABASE_UPDATED_AT_K, filter);
        }

        List<Document> res = MongoDAO.find(Reader.getConfiguration().getCatalogDatabase(), DATABASE_CATALOG, query,
                fields(include(DATABASE_NAME_K, DATABASE_UPDATED_AT_K), excludeId()));

        //Modify the longs to be human readable (Probably should modify the way of saving the dates)...
        res.forEach(doc -> doc.put(DATABASE_UPDATED_AT_K, Instant.ofEpochMilli(doc.getLong(DATABASE_UPDATED_AT_K))));

        return res;
    }

    /**
     * Get the metadata of the grid associated with this database/data-set, this includes the origin coordinate,
     * width, height, among other properties. It could be considered as the spatial coverage of the grid
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return a {@link Document} object representing the grid metadata
     */
    public static Document getGridMetadata(String databaseName) {
        String DATABASE_SPAT_COV_K = "spatial_coverage";
        Document meta = findDatabase(databaseName, fields(include(DATABASE_SPAT_COV_K), excludeId()));

        return (meta != null) ? meta.get(DATABASE_SPAT_COV_K, Document.class) : null;
    }

    /**
     * Get the temporal coverage of this database/data-set. This includes the start time and end time of the period
     * covered
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return a {@link Document} object with the temporal coverage (start and end of period)
     */
    public static Document getTemporalCoverage(String databaseName) {
        String DATABASE_TEMP_COV_K = "temporal_coverage";
        Document res = findDatabase(databaseName, fields(include(DATABASE_TEMP_COV_K), excludeId()));

        return (res != null) ? res.get(DATABASE_TEMP_COV_K, Document.class) : null;
    }

    /**
     * Get the metadata of the variables available in this database/data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return a list of {@link Document} objects representing the metadata of all the available variables
     */
    public static List<Document> getVariables(String databaseName) {
        String DATABASE_VARS_K = "variables";
        Document res = findDatabase(databaseName, fields(include(DATABASE_VARS_K), excludeId()));

        return (res != null) ? res.get(DATABASE_VARS_K, List.class) : null;
    }

    /**
     * Get the total times (3rd dimension length) available for this data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the total times available
     */
    public static int getTotalTimes(String databaseName) {
        String DATABASE_TIMES_K = "total_times";
        Document res = findDatabase(databaseName, fields(include(DATABASE_TIMES_K), excludeId()));

        return (res != null) ? res.getInteger(DATABASE_TIMES_K) : -1;
    }

    /**
     * Get the total latitudes (2nd dimension length) available for this data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the total latitudes available
     */
    public static int getTotalLatitudes(String databaseName) {
        String DATABASE_LATS_K = "total_lats";
        Document res = findDatabase(databaseName, fields(include(DATABASE_LATS_K), excludeId()));

        return (res != null) ? res.getInteger(DATABASE_LATS_K) : -1;
    }

    /**
     * Get the total longitudes (1st dimension length) available for this data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the total longitudes available
     */
    public static int getTotalLongitudes(String databaseName) {
        String DATABASE_LONGS_K = "total_longs";
        Document res = findDatabase(databaseName, fields(include(DATABASE_LONGS_K), excludeId()));

        return (res != null) ? res.getInteger(DATABASE_LONGS_K) : -1;
    }

    /**
     * Get the total height levels (Z index, 3rd spatial dimension) available for this data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the total height levels available
     */
    public static int getTotalHeights(String databaseName) {
        String DATABASE_HEIGHTS_K = "total_heights";
        Document res = findDatabase(databaseName, fields(include(DATABASE_HEIGHTS_K), excludeId()));

        return (res != null) ? res.getInteger(DATABASE_HEIGHTS_K) : -1;
    }

    /**
     * Get the spatial resolution of the grid associated with this database/data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the spatial resolution of this grid as a {@link Document} object with the latitude and longitude
     * resolutions
     */
    public static Document getSpatialResolution(String databaseName) {
        String DATABASE_SPAT_RES_K = "spatial_resolution";
        Document res = findDatabase(databaseName, fields(include(DATABASE_SPAT_RES_K), excludeId()));

        return (res != null) ? res.get(DATABASE_SPAT_RES_K, Document.class) : null;
    }

    /**
     * Get the temporal resolution of the grid associated with this database/data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the temporal resolution of this grid as a ISO text representing the duration
     */
    public static String getTemporalResolution(String databaseName) {
        String DATABASE_TEMP_RES_K = "temporal_resolution";
        Document res = findDatabase(databaseName, fields(include(DATABASE_TEMP_RES_K), excludeId()));

        return (res != null) ? res.getString(DATABASE_TEMP_RES_K) : "";
    }

}