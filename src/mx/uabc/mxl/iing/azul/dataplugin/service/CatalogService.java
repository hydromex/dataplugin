package mx.uabc.mxl.iing.azul.dataplugin.service;

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CatalogManager;
import mx.uabc.mxl.iing.azul.dataplugin.datastore.RasterDataManager;
import mx.uabc.mxl.iing.azul.dataplugin.datastore.RasterManagerFactory;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;

@Path("/meta")
public class CatalogService {

    private static final String DATABASE_NAME_K = "name";
    private static final String DATABASE_SCHEMA_K = "db_schema";
    private static final String DATABASE_TYPE_K = "db_type";
    private static final String DATABASE_VARIABLES_K = "variables";
    private static final String GRID_TOTAL_LONGS_K = "total_longs";
    private static final String GRID_TOTAL_LATS_K = "total_lats";
    private static final String GRID_TOTAL_HEIGHTS = "total_heights";
    private static final String GRID_TOTAL_TIMES = "total_times";
    private static final String GRID_SPAT_COVERAGE = "spatial_coverage";
    private static final String GRID_TEMP_COVERAGE = "temporal_coverage";
    private static final String GRID_SPAT_RES = "spatial_resolution";
    private static final String GRID_TEMP_RES = "temporal_resolution";
    private static final String DATABASE_STORAGE_DBMS = "storage_dbms";
    //Put later lats, lons, times and heights arrays with the actual metadata.


    private static final String VARIABLE_NAME_K = "name";

    private enum DB_TYPE {CLIMATIC_GRID, CLIMATIC_POINTS}

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

    @Path("register-grid-db")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String registerGridDatabase(@FormParam("db-name") String dbName, @FormParam("plugin-name") String pluginName,
                                       @FormParam("plugin-version") String pluginVersion,
                                       @FormParam("variables") String vars, @FormParam("dbms") String dbms) {
        //Build metadata for this database...
        //Get the needed raster metadata from the specified DBMS
        RasterDataManager rasterManager = RasterManagerFactory.getRasterManager(dbms);

        //Later add metadata of each variable from og format, such as {name, longname, units, missing_val, extra: {}}
        JSONArray variables = new JSONArray(Arrays.stream(vars.split(",")).map(var -> new JSONObject()
                .put(VARIABLE_NAME_K, var)).toArray());

        //Get the name of the first variable to get the data from its raster...
        String varName = variables.getJSONObject(0).getString(VARIABLE_NAME_K);

        JSONObject database = new JSONObject().put(DATABASE_NAME_K, dbName)
                .put(DATABASE_TYPE_K, DB_TYPE.CLIMATIC_GRID.name()).put(DATABASE_VARIABLES_K, variables)
                .put(GRID_TOTAL_LONGS_K, rasterManager.getWidth(dbName, varName))
                .put(GRID_TOTAL_LATS_K, rasterManager.getHeight(dbName, varName))
                .put(GRID_TOTAL_HEIGHTS, rasterManager.getBands(dbName, varName))
                .put(GRID_TOTAL_TIMES, rasterManager.getTotalTimes(dbName))
                .put(GRID_SPAT_COVERAGE, rasterManager.getMetadata(dbName, varName))
                .put(GRID_TEMP_COVERAGE, rasterManager.getTemporalCoverage(dbName))
                .put(GRID_SPAT_RES, rasterManager.getSpatialRes(dbName, varName))
                .put(GRID_TEMP_RES, rasterManager.getTemporalRes(dbName)).put(DATABASE_STORAGE_DBMS, dbms)
                .put(DATABASE_SCHEMA_K, rasterManager.getStorageSchema(dbName));
        //put also a JSONObject of extra_meta in case of global attributes in og dataset.


        CatalogManager.registerDatabase(pluginName, pluginVersion, Document.parse(database.toString()));

        return "Database [" + dbName + "] registered/updated with plugin [" + pluginName + "]:" + pluginVersion;
    }
}