package mx.uabc.mxl.iing.azul.dataplugin.service;
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

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CatalogManager;
import mx.uabc.mxl.iing.azul.dataplugin.datastore.RasterDataManager;
import mx.uabc.mxl.iing.azul.dataplugin.datastore.RasterManagerFactory;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Restful resource used as the entry point to the public API grid metadata related services
 *
 * @author jdosornio
 * @version %I%
 */
@Path("/grid-meta")
public class CatalogService {

    private static final String DATABASE_NAME_K = "name";
    private static final String DATABASE_SCHEMA_K = "db_schema";
    private static final String DATABASE_TYPE_K = "db_type";
    private static final String DATABASE_VARIABLES_K = "variables";
    private static final String GRID_TOTAL_LONGS_K = "total_longs";
    private static final String GRID_TOTAL_LATS_K = "total_lats";
    private static final String GRID_TOTAL_HEIGHTS_K = "total_heights";
    private static final String GRID_TOTAL_TIMES_K = "total_times";
    private static final String GRID_SPAT_COVERAGE_K = "spatial_coverage";
    private static final String GRID_TEMP_COVERAGE_K = "temporal_coverage";
    private static final String GRID_SPAT_RES_K = "spatial_resolution";
    private static final String GRID_TEMP_RES_K = "temporal_resolution";
    private static final String DATABASE_STORAGE_DBMS_K = "storage_dbms";

    private static final String GRID_ORIGIN_X_K = "upperleftx";
    private static final String GRID_ORIGIN_Y_K = "upperlefty";
    //Put later lats, lons, times and heights arrays with the actual metadata.


    private static final String VARIABLE_NAME_K = "name";

    private enum DB_TYPE {CLIMATIC_GRID, CLIMATIC_POINTS}

    /**
     * Method to be used by the plugins to register/update a database/data-set metadata in the plugin catalog.
     * It registers data such as the database/data-set name, the database schema, and the plugin name and version
     *
     * @param dbName the name of the database/data-set to be registered
     * @param dbSchema a JSON string containing the underlying database schema for this data-set
     * @param pluginName the name of the plugin that loaded the data into Dataplugin
     * @param pluginVersion the version of the plugin that loaded the data into Dataplugin
     *
     * @return a message with the database/data-set, plugin name and version registered
     */
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

    /**
     * This method is more sophisticated than the registerDatabase one, because it registers the database/data-set
     * metadata in an automatic way, only providing the database/data-set name, the plugin name and version, the
     * variables stored and the DBMS used to do that. This method is focused to gridded data-sets, so it only works
     * which such types. The data must be already loaded into the application grid database
     *
     * @param dbName the name of the database/data-set to be registered
     * @param pluginName the name of the plugin that loaded the data into Dataplugin
     * @param pluginVersion the version of the plugin that loaded the data into Dataplugin
     * @param vars a list of variable names, separated by commas. (Later it could be a JSON array with more metadata)
     * @param dbms the name of the DBMS where the data was stored (Only PostgreSQL supported by now)
     *
     * @return a message with the database/data-set, plugin name and version registered
     */
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
                .put(GRID_TOTAL_HEIGHTS_K, rasterManager.getBands(dbName, varName))
                .put(GRID_TOTAL_TIMES_K, rasterManager.getTotalTimes(dbName))
                .put(GRID_SPAT_COVERAGE_K, rasterManager.getMetadata(dbName, varName))
                .put(GRID_TEMP_COVERAGE_K, rasterManager.getTemporalCoverage(dbName))
                .put(GRID_SPAT_RES_K, rasterManager.getSpatialRes(dbName, varName))
                .put(GRID_TEMP_RES_K, rasterManager.getTemporalRes(dbName)).put(DATABASE_STORAGE_DBMS_K, dbms)
                .put(DATABASE_SCHEMA_K, rasterManager.getStorageSchema(dbName));
        //put also a JSONObject of extra_meta in case of global attributes in og dataset.


        CatalogManager.registerDatabase(pluginName, pluginVersion, Document.parse(database.toString()));

        return "Database [" + dbName + "] registered/updated with plugin [" + pluginName + "]:" + pluginVersion;
    }

    /**
     * Gets the metadata associated to the database/data-set with the provided name
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return a JSON string with the database/data-set metadata
     */
    @Path("get-db/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getDatabaseMetadata(@PathParam("name") String databaseName) {
        Document db = CatalogManager.findDatabase(databaseName);


        //Return an empty JSON object if database not found...
        return (db != null) ? db.toJson() : "{}";
    }

    /**
     * Obtains the database/data-set metadata stored in the catalog, optionally filtering by a period of time
     * in the last modification date
     *
     * @param newerThan a string with a starting date in the format YYYY-MM-DD. Example: 2018-06-18
     * @param olderThan a string with a ending date in the format YYYY-MM-DD. Example: 2018-06-20
     *
     * @return a JSON array with the name and last modification date of the database/data-sets queried
     */
    @Path("list-db")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getDatabases(@QueryParam("newer-than") String newerThan,
                               @QueryParam("older-than") String olderThan) {
        LocalDate stDt = null;
        LocalDate edDt = null;

        try {
            if(newerThan != null && !newerThan.isEmpty()) {
                stDt = LocalDate.parse(newerThan);
            }

            if(olderThan != null && !olderThan.isEmpty()) {
                edDt = LocalDate.parse(olderThan);
            }
        } catch (DateTimeParseException e) {
            MessageMediator.sendMessage("ERROR! couldn't parse the dates correctly: " + e,
                    MessageMediator.ERROR_MESSAGE);
        }


        return new JSONArray(CatalogManager.getDatabases(stDt, edDt)).toString();
    }

    /**
     * Get the metadata of the grid associated with this database/data-set, this includes the origin coordinate,
     * width, height, among other properties. It could be considered as the spatial coverage of the grid
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return a JSON formatted string representing the grid metadata
     */
    @Path("get-grid-meta/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getGridMetadata(@PathParam("name") String databaseName) {
        //Get grid coverage / metadata from metadata catalog
        Document metadata = CatalogManager.getGridMetadata(databaseName);


        return (metadata != null) ? metadata.toJson() : "{}";
    }

    /**
     * Get the temporal coverage of this database/data-set. This includes the start time and end time of the period
     * covered
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return a JSON string with the temporal coverage (start and end of period)
     */
    @Path("get-temp-cov/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getTemporalCoverage(@PathParam("name") String databaseName) {
        Document tempCov = CatalogManager.getTemporalCoverage(databaseName);

        return (tempCov != null) ? tempCov.toJson() : "{}";
    }

    /**
     * Get the metadata of the variables available in this database/data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return a JSON array with the metadata of all the available variables
     */
    @Path("get-vars/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getVariables(@PathParam("name") String databaseName) {
        List<Document> vars = CatalogManager.getVariables(databaseName);

        return (vars != null) ? new JSONArray(vars).toString() : "[]";
    }

    /**
     * Get the total times (3rd dimension length) available for this data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the total times available
     */
    @Path("get-times/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getTotalTimes(@PathParam("name") String databaseName) {
        return String.valueOf(CatalogManager.getTotalTimes(databaseName));
    }

    /**
     * Get the total latitudes (2nd dimension length) available for this data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the total latitudes available
     */
    @Path("get-lats/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getTotalLatitudes(@PathParam("name") String databaseName) {
        return String.valueOf(CatalogManager.getTotalLatitudes(databaseName));
    }

    /**
     * Get the total longitudes (1st dimension length) available for this data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the total longitudes available
     */
    @Path("get-longs/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getTotalLongitudes(@PathParam("name") String databaseName) {
        return String.valueOf(CatalogManager.getTotalLongitudes(databaseName));
    }

    /**
     * Get the total height levels (Z index, 3rd spatial dimension) available for this data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the total height levels available
     */
    @Path("get-heights/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getTotalHeightLevels(@PathParam("name") String databaseName) {
        return String.valueOf(CatalogManager.getTotalHeights(databaseName));
    }

    /**
     * Gets the grid origin, that is, the upper left coordinate (longitude, latitude)
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return a JSON string with the origin coordinate
     */
    @Path("get-origin/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getGridOrigin(@PathParam("name") String databaseName) {
        Document meta = CatalogManager.getGridMetadata(databaseName);

        if (meta == null) {
            //no result
            return "{}";
        } else {
            return new JSONObject().put("long", meta.get(GRID_ORIGIN_X_K))
                    .put("lat", meta.get(GRID_ORIGIN_Y_K)).toString();
        }
    }

    /**
     * Get the spatial resolution of the grid associated with this database/data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the spatial resolution of this grid as a JSON string with the latitude and longitude resolutions
     */
    @Path("get-spat-res/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getSpatialResolution(@PathParam("name") String databaseName) {
        Document spatRes = CatalogManager.getSpatialResolution(databaseName);

        return (spatRes != null) ? spatRes.toJson() : "{}";
    }

    /**
     * Get the temporal resolution of the grid associated with this database/data-set
     *
     * @param databaseName the name of the database/data-set to search for
     *
     * @return the temporal resolution of this grid as a ISO text representing the duration
     */
    @Path("get-temp-res/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTemporalResolution(@PathParam("name") String databaseName) {
        return CatalogManager.getTemporalResolution(databaseName);
    }

}