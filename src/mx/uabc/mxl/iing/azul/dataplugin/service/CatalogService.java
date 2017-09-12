package mx.uabc.mxl.iing.azul.dataplugin.service;

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CatalogManager;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import org.bson.Document;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/meta")
public class CatalogService {

    private static final String DATABASE_NAME_K = "name";
    private static final String DATABASE_SCHEMA_K = "db_schema";

    @Path("register-db")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String registerDatabase(@FormParam("db-name") String dbName, @FormParam("db-schema") String dbSchema,
                                   @FormParam("plugin-name") String pluginName,
                                   @FormParam("plugin-version") String pluginVersion) {

        //Validate in some way the dbSchema...
        Document database = new Document(DATABASE_NAME_K, dbName);
        if (dbSchema != null && !dbSchema.isEmpty()) {
            database.append(DATABASE_SCHEMA_K, Document.parse(dbSchema));
        } else {
            MessageMediator.sendMessage("no db-schema found!");
        }

        CatalogManager.registerDatabase(pluginName, pluginVersion, database);

        return "Database [" + dbName + "] registered/updated with plugin [" + pluginName + "]:" + pluginVersion;
    }
}