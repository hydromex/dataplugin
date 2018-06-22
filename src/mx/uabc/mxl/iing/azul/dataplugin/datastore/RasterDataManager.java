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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This interface is used to declare the methods available to the public and that the following
 * RasterDataManager extensions must implement. These methods range from getting plugin and database/data-set
 * metadata, to getting the actual grid data, given some query filters
 *
 * @author jdosornio
 * @version %I%
 */
public interface RasterDataManager {
    /**
     * Gets the raster metadata (width, height, origin, etc) with the given database and variable names
     *
     * @param database the name of the database to look for
     * @param variable the name of the variable to look for
     *
     * @return a map object containing the raster metadata
     */
    Map<String, Object> getMetadata(String database, String variable);

    /**
     * Gets the raster width, given a database and variable names
     *
     * @param database the name of the database to look for
     * @param variable the name of the variable to look for
     *
     * @return the raster width
     */
    int getWidth(String database, String variable);

    /**
     * Gets the raster height, given a database and variable names
     *
     * @param database the name of the database to look for
     * @param variable the name of the variable to look for
     *
     * @return the raster height
     */
    int getHeight(String database, String variable);

    /**
     * Gets the raster number of bands, given a database and variable names
     *
     * @param database the name of the database to look for
     * @param variable the name of the variable to look for
     *
     * @return the raster number of bands
     */
    int getBands(String database, String variable);

    /**
     * Gets the raster origin, given a database and variable names
     *
     * @param database the name of the database to look for
     * @param variable the name of the variable to look for
     *
     * @return the raster origin as a map object containing longitude and latitude
     */
    Map<String, Double> getOrigin(String database, String variable);

    /**
     * Gets the raster spatial resolution, given a database and variable names
     *
     * @param database the name of the database to look for
     * @param variable the name of the variable to look for
     *
     * @return the raster spatial resolution as a map object containing the resolution for longitude and latitude
     */
    Map<String, Double> getSpatialRes(String database, String variable);

    /**
     * Gets the raster temporal coverage, given a database name
     *
     * @param database the name of the database to look for
     *
     * @return the raster temporal coverage as a map object containing the start and end dates
     */
    Map<String, Date> getTemporalCoverage(String database);

    /**
     * Gets the total times available in a database, given its name
     *
     * @param database the name of the database to search for
     *
     * @return the total times available in the database
     */
    long getTotalTimes(String database);

    /**
     * Gets the raster temporal resolution, given a database name
     *
     * @param database the name of the database to look for
     *
     * @return the raster temporal resolution as a duration object
     */
    Duration getTemporalRes(String database);

    /**
     * This is the method needed to extract raster data following spatial and temporal filters. It gets data from
     * a specified database and variable, optionally querying the data by start and end time and within a polygon.
     * It also has the options to select the output format and aggregate operation. It can also send the data to
     * a local file
     *
     * @param database the name of the database from where to extract the data
     * @param variable the data variable name
     * @param startTime the start date of the temporal filter, may be null
     * @param endTime the end date of the temporal filter, may be null
     * @param withinPolygon the polygon containing the data (spatial filter), may be null
     * @param outputFormat the preferred output format, default: raw
     * @param aggOp the preferred aggregation operation, default: mean
     * @param outputFile if specified, send the data to a local file instead of returning it through a byte array
     *
     * @return the queried data as a byte array if an output file isn't specified, else null
     */
    byte[] getRasterData(String database, String variable, LocalDateTime startTime,
                         LocalDateTime endTime, Geometry withinPolygon, String outputFormat,
                         String aggOp, File outputFile);

    /**
     * Returns the data from just a point (or approximation of it) in the raster, it could return an array of
     * numbers in case of selecting more than one time through a temporal range
     *
     * @param database the name of the database from where to extract the data
     * @param variable the data variable name
     * @param point the point from where to extract the variable value(s)
     * @param startTime the start date of the temporal filter, may be null
     * @param endTime the end date of the temporal filter, may be null
     *
     * @return an array of numbers with the queried data
     */
    Number[] getDataFromPoint(String database, String variable, Point point,
                              LocalDateTime startTime, LocalDateTime endTime);

    /**
     * This is a convenience method used to generate the physical storage schema of a given raster database,
     * according to the implementing technology or DBMS
     *
     * @param database the name of the database from which to get the underlying storage schema
     *
     * @return a list of map objects containing the storage schema structure
     */
    List<Map<String, Object>> getStorageSchema(String database);
}