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

import mx.uabc.mxl.iing.azul.dataplugin.datastore.RasterDataManager;
import mx.uabc.mxl.iing.azul.dataplugin.datastore.RasterManagerFactory;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import org.json.JSONArray;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Restful resource used as the entry point to the public API grid data related services
 *
 * @author jdosornio
 * @version %I%
 */
@Path("/grid-data")
public class GridDataService {

    private static final String POSTGRES_MANAGER = "postgresql";


    /**
     * Gets the grid data of the database/data-set and variable specified, optionally filtering the grid through
     * spatial and time dimensions. In case of time it will be filtered by a time period, as for the spatial
     * dimension, it will be filtered by a containing polygon.
     * The returned data can be either raw (PostGIS raster bytes) or a GDAL supported format such as GTiff.
     * If getting more than one grid (time) in the query, then the resulting grids are merged into one by the
     * specified aggregate operation (MEAN by default)
     *
     * @param databaseName the name of the database/data-set from where to extract the data
     * @param variableName the variable name of the data of interest
     * @param startTime starting time, in the ISO format YYYY-MM-DDThh:mm:ss, may be null. Example: 2018-06-20T10:28:25
     * @param endTime ending time, in the ISO format YYYY-MM-DDThh:mm:ss, may be null. Example: 2018-07-05T11:00:00
     * @param polygon the bounding polygon containing the data of interest. Must be a WKT String with a closing polygon.
     * Example: POLYGON((10.5 20.7, 30.8 18.7, 20.34 10.4, 10.5 20.7)), may be null
     * @param format the name of the desired data output format. Default: raw
     * @param aggregateOperation the name of the aggregation operation to apply to the result. Default: MEAN.
     * Available: SUM, COUNT, LAST, ...
     *
     * @return the resulting grid bytes, raw or format encoded, representing the queried variable in the required
     * time and space subset
     */
    @Path("get/{name}/{var}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public static byte[] getGridData(@PathParam("name") String databaseName, @PathParam("var") String variableName,
                                     @QueryParam("st-time") String startTime, @QueryParam("ed-time") String endTime,
                                     @QueryParam("within") String polygon, @QueryParam("format") String format,
                                     @QueryParam("agg-op") String aggregateOperation) {

        //Right now only one DBMS supported, so...
        RasterDataManager rasMan = RasterManagerFactory.getRasterManager(POSTGRES_MANAGER);
        LocalDateTime stTime = null;
        LocalDateTime edTime = null;
        Geometry withinPolygon = null;
        byte[] grid = null;

        try {
            if(startTime != null && !startTime.isEmpty()) {
                stTime = LocalDateTime.parse(startTime);
            }

            if (endTime != null && !endTime.isEmpty()) {
                edTime = LocalDateTime.parse(endTime);
            }

            if (polygon != null && !polygon.isEmpty()) {
                withinPolygon = new WKTReader().read(polygon);
            }

            //mean aggregation by default...
            if(aggregateOperation == null || aggregateOperation.isEmpty()) {
                aggregateOperation = "mean";
            }

            grid = rasMan.getRasterData(databaseName, variableName, stTime, edTime, withinPolygon, format,
                    aggregateOperation, null);
        } catch(DateTimeParseException ex) {
            MessageMediator.sendMessage("Failed to parse the date correctly!: " + ex,
                    MessageMediator.ERROR_MESSAGE);
        } catch (ParseException | IllegalArgumentException ex) {
            MessageMediator.sendMessage("Failed to parse the polygon correctly!: " + ex,
                    MessageMediator.ERROR_MESSAGE);
        }


        return (grid != null) ? grid : new byte[]{};
    }

    /**
     * Gets the data of the database/data-set, variable and point specified, optionally filtering through time,
     * by a time period. If more than one time is requested, then this method will return a JSON array containing the
     * values for the specified time period at that specific point
     *
     * @param databaseName the name of the database/data-set from where to extract the data
     * @param variableName the variable name of the data of interest
     * @param point the point (or an approximation of it) from where to extract the data. Must be a WKT String
     *      * Example: POINT((10.5 20.7))
     * @param startTime starting time, in the ISO format YYYY-MM-DDThh:mm:ss, may be null. Example: 2018-06-20T10:28:25
     * @param endTime ending time, in the ISO format YYYY-MM-DDThh:mm:ss, may be null. Example: 2018-07-05T11:00:00
     *
     * @return a JSON array containing the requested point data
     */
    @Path("from-point/{name}/{var}/{point}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public static String getDataFromPoint(@PathParam("name") String databaseName, @PathParam("var") String variableName,
                                     @PathParam("point") String point, @QueryParam("st-time") String startTime,
                                          @QueryParam("ed-time") String endTime) {

        //Right now only one DBMS supported, so...
        RasterDataManager rasMan = RasterManagerFactory.getRasterManager(POSTGRES_MANAGER);
        LocalDateTime stTime = null;
        LocalDateTime edTime = null;
        Point pointGeom = null;
        Number[] pointData = null;

        try {
            if(startTime != null && !startTime.isEmpty()) {
                stTime = LocalDateTime.parse(startTime);
            }

            if (endTime != null && !endTime.isEmpty()) {
                edTime = LocalDateTime.parse(endTime);
            }

            if (point != null && !point.isEmpty()) {
                pointGeom = (Point) new WKTReader().read(point);
            }

            pointData = rasMan.getDataFromPoint(databaseName, variableName, pointGeom, stTime, edTime);

        } catch(DateTimeParseException ex) {
            MessageMediator.sendMessage("Failed to parse the date correctly!: " + ex,
                    MessageMediator.ERROR_MESSAGE);
        } catch (ParseException | IllegalArgumentException | ClassCastException ex) {
            MessageMediator.sendMessage("Failed to parse the point correctly!: " + ex,
                    MessageMediator.ERROR_MESSAGE);
        }


        return (pointData != null) ? new JSONArray(pointData).toString() : "[]";
    }

}