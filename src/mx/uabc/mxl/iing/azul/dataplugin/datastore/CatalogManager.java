package mx.uabc.mxl.iing.azul.dataplugin.datastore;

import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Reader;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;
import org.bson.Document;

import java.util.Iterator;

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


    public static void registerPlugin(Plugin plugin) {
        //See if the same name and version of the plugin already exists, if so don't create it again...
        if (findPlugin(plugin.getName(), plugin.getVersion()) == null) {
            Document pluginMetadata = new Document(plugin.getMetadata().asMap());
            MongoDAO.insert(Reader.getConfiguration().getCatalogDatabase(), PLUGIN_CATALOG, pluginMetadata);
            MessageMediator.sendMessage("Plugin: [" + plugin.getName() + "] registered in catalog");
        }
    }

    public static void registerDatabase(String pluginName, String pluginVersion, Document database) {
        //Must validate db_schema document in some way... At least for the required fields...
        //Almost the same logic as plugin, just that, in this case, the field updated_at is updated
        Document db = findDatabase(database.getString(DATABASE_NAME_K));
        if (db != null) {

            //Update updated_at field
            MongoDAO.update(Reader.getConfiguration().getCatalogDatabase(), DATABASE_CATALOG,
                    new Document(DATABASE_NAME_K, db.getString(DATABASE_NAME_K)),
                    new Document("$set", new Document(DATABASE_UPDATED_AT_K, System.currentTimeMillis())));
            MessageMediator.sendMessage("Updated database: [" + db.getString(DATABASE_NAME_K) + "]");

        } else {
            //If the database does not exists, add a created at field with the current timestamp.
            //Before inserting, create a created_at field and add the id of the plugin...
            long currTime = System.currentTimeMillis();
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

    public static Document findPlugin(String name, String version) {
        Iterator<Document> it = MongoDAO.find(Reader.getConfiguration().getCatalogDatabase(), PLUGIN_CATALOG,
                new Document(PLUGIN_NAME_K, name).append(PLUGIN_VERSION_K, version));

        return (it.hasNext()) ? it.next() : null;
    }

    public static Document findDatabase(String name) {
        Iterator<Document> it = MongoDAO.find(Reader.getConfiguration().getCatalogDatabase(), DATABASE_CATALOG,
                new Document(DATABASE_NAME_K, name));

        return (it.hasNext()) ? it.next() : null;
    }
}