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

import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Reader;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import org.locationtech.jts.geom.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;

/**
 * This class is a direct implementation of the RasterDataManager interface. It is used to provide the required
 * functionality through the PostgreSQL DBMS and its PostGIS extension for raster data storage
 *
 * @author jdosornio
 * @version %I%
 */
public class PostgisRasterDAO implements RasterDataManager {
    //TODO: Use connection pooling
    private static Connection PG_CON;
    private static final String DRIVER = "org.postgresql.Driver";
    private static final String USER = Reader.getConfiguration().getPostgresUser();
    private static final String PASS = Reader.getConfiguration().getPostgresPass();
    private static final String URL = "jdbc:postgresql://" + Reader.getConfiguration().getPostgresHost() +
            ":" + Reader.getConfiguration().getPostgresPort() + "/" + Reader.getConfiguration().getPostgresGridDB();

    private static final String TIME_COL = "datetime";

    static {
        try {
            Class.forName(DRIVER);
            PG_CON = DriverManager.getConnection(URL, USER, PASS);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new PostgisRasterDAO instance
     */
    PostgisRasterDAO() {

    }

    /**
     * Closes the resources associated to a SQL query
     *
     * @param ps prepared statement, may be null
     * @param rs result set, may be null
     *
     * @throws SQLException if there's an error with the operation
     */
    private static void close(PreparedStatement ps, ResultSet rs) throws SQLException {
        if (ps != null) {
            ps.close();
        }
        if (rs != null) {
            rs.close();
        }
    }

    /**
     * This is a convenience method to transform a ResultSet object to a Map object
     *
     * @param query the query to run
     *
     * @return the query result as a map object
     *
     * @throws SQLException if there's an error
     */
    private static Map<String, Object> resultSetToMap(String query) throws SQLException {

        PreparedStatement ps = PG_CON.prepareStatement(query);
        ResultSet rs = ps.executeQuery();

        rs.next();
        //Save all columns to a map (single row)
        Map<String, Object> row = new HashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
        }

        close(ps, rs);

        return row;
    }

    public Map<String, Object> getMetadata(String table, String rasterCol) {
        //datetime column = time column
        String query = "SELECT (ST_Metadata(ST_Union(" + rasterCol +
                "))).* FROM " + table + " GROUP BY " + TIME_COL + " LIMIT 1";

        try {
            return resultSetToMap(query);
        } catch (SQLException e) {
            MessageMediator.sendMessage("Error! when retrieving metadata: " + e,
                    MessageMediator.ERROR_MESSAGE);
        }

        return null;
    }

    public int getWidth(String table, String rasterCol) {
        return (int) getMetadata(table, rasterCol).get("width");
    }

    public int getHeight(String table, String rasterCol) {
        return (int) getMetadata(table, rasterCol).get("height");
    }

    public int getBands(String table, String rasterCol) {
        return (int) getMetadata(table, rasterCol).get("numbands");
    }

    public Map<String, Double> getOrigin(String table, String rasterCol) {
        Map<String, Double> origin = new HashMap<>();
        Map<String, Object> metadata = getMetadata(table, rasterCol);

        origin.put("x", (double) metadata.get("upperleftx"));
        origin.put("y", (double) metadata.get("upperlefty"));

        return origin;
    }

    public Map<String, Double> getSpatialRes(String table, String rasterCol) {
        Map<String, Double> spatialRes = new HashMap<>();
        Map<String, Object> metadata = getMetadata(table, rasterCol);

        spatialRes.put("lon", (double) metadata.get("scalex"));
        spatialRes.put("lat", (double) metadata.get("scaley"));

        return spatialRes;
    }

    public Map<String, Date> getTemporalCoverage(String table) {
        String query = "SELECT MIN(" + TIME_COL + ") AS begin_time, MAX(" + TIME_COL + ") AS end_time FROM " + table;

        try {
            Map<String, Object> res = resultSetToMap(query);
            Map<String, Date> tempCov = new HashMap<>();

            res.forEach((key, value) -> tempCov.put(key, (Date)value));

            return tempCov;
        } catch (SQLException e) {
            MessageMediator.sendMessage("Error when retrieving temporal coverage!: " + e,
                    MessageMediator.ERROR_MESSAGE);
        }

        return null;
    }

    public long getTotalTimes(String table) {
        String query = "SELECT COUNT(DISTINCT(" + TIME_COL + ")) AS times FROM " + table;

        try {
            return (long) resultSetToMap(query).get("times");
        } catch (SQLException e) {
            MessageMediator.sendMessage("Error when retrieving total times!: " + e,
                    MessageMediator.ERROR_MESSAGE);
        }

        return -1;
    }

    public Duration getTemporalRes(String table) {
        //Kind of get the average duration between each timestep, although it must be constant... (Ej. Monthly but may be 31 to 28 days).
        String query = "SELECT EXTRACT (epoch FROM (MAX(" + TIME_COL + ") - MIN(" + TIME_COL + ")) / " +
                "(COUNT(DISTINCT(" + TIME_COL + ")) - 1)) AS t_res FROM " + table;

        try {
            return Duration.ofSeconds(((Double)resultSetToMap(query).get("t_res")).longValue());
        } catch (SQLException e) {
            MessageMediator.sendMessage("Error when retrieving temporal resolution!: " + e,
                    MessageMediator.ERROR_MESSAGE);
        }

        return null;
    }

    /**
     * This is a utility method used to dynamically construct a PostgreSQL query with the specified constraints
     *
     * @param table the PostgreSQL table name
     * @param rasterCol the table raster column name
     * @param startTime the starting date to be used in the query, may be null
     * @param endTime the ending date to be used in the query, may be null
     * @param withinPolygon the bounding polygon to be used in the query, may be null
     * @param outputFormat the output format requested, default: raw
     * @param aggOp the aggregation operation, default: mean
     *
     * @return a string containing the generated PostgreSQL query
     */
    //Get raster data...
    //String polygon could be actual polygon data type. But not needed for now
    private static String getSpatioTemporalQuery(String table, String rasterCol, LocalDateTime startTime,
                                                 LocalDateTime endTime, Geometry withinPolygon, String outputFormat,
                                                 String aggOp) {
        if (table == null || table.isEmpty() || rasterCol == null || rasterCol.isEmpty()) {
            MessageMediator.sendMessage("Error! table and rasterCol are obligatory!",
                    MessageMediator.ERROR_MESSAGE);
            return null;
        }

        final Set<String> VALID_AGG_OPS = new HashSet<>(Arrays.asList("LAST", "FIRST", "MIN", "MAX", "COUNT",
                "SUM", "MEAN", "RANGE"));
        //Instead of also validating the output format, is better to just let java catch the exception at query time
        //but it's okay to just validate this case:
        if(outputFormat != null && outputFormat.equalsIgnoreCase("raw")) {
            outputFormat = null;
        }

        //Validate if valid aggregate operation...
        if(aggOp != null && !VALID_AGG_OPS.contains(aggOp.toUpperCase())) {
            aggOp = null;
        }

        aggOp = (aggOp != null) ? ", '" + aggOp + "'" : "";

        String baseQuery = "SELECT ST_Union(" + rasterCol + aggOp + ") FROM " + table /*+ " GROUP BY " + TIME_COL +
                " ORDER BY " + TIME_COL*/;  //For now, don't group by datetime, only aggregate everything into one raster
        //Add temporal restrictions if there exist
        String temporalFilter = "";
        String spatialFilter = "";
        if (startTime != null && endTime != null) {
            temporalFilter = "("+ TIME_COL + " BETWEEN '" + startTime + "' AND '" + endTime + "')";
        } else if (startTime != null) {
            //Only startTime
            temporalFilter = "(" + TIME_COL + " >= '" + startTime + "')";
        } else if (endTime != null) {
            //Only endTime
            temporalFilter = "(" + TIME_COL + " <= '" + endTime + "')";
        }

        //Add spatial restrictions if there exist
        //Validate if polygon exists and isn't empty
        if (withinPolygon != null && !withinPolygon.isEmpty()) {
            String polygon = "ST_GeomFromText('" + withinPolygon + "')";

            //modify query to clip polygon area from raster
            baseQuery = baseQuery.replaceAll("ST_Union\\((\\S+)(,\\s*'\\w+')?\\)",
                    "ST_Union(ST_Clip($1, " + polygon + ")$2)");
            spatialFilter = "(" + rasterCol + " && " + polygon + ")";
        }

        //Add all restrictions...
        String filter;
        if (!temporalFilter.isEmpty() && !spatialFilter.isEmpty()) {
            filter = temporalFilter + " AND " + spatialFilter;
        } else {
            filter = temporalFilter + spatialFilter;
        }

        baseQuery = (!filter.isEmpty()) ? baseQuery.replaceAll("(FROM\\s+\\S+)$",
                "$1 WHERE " + filter) : baseQuery;

        //Export to a file format...
        if (outputFormat != null && !outputFormat.isEmpty()) {
            //Later call a stored procedure or something that correctly adds the time information missing...
            baseQuery = baseQuery.replaceAll("(ST_Union\\(.+\\))\\s+FROM", "ST_AsGDALRaster($1, '" +
                    outputFormat + "') FROM");
        }


        MessageMediator.sendMessage(baseQuery);

        return baseQuery;
    }

    public byte[] getRasterData(String table, String rasterCol, LocalDateTime startTime,
                                     LocalDateTime endTime, Geometry withinPolygon, String outputFormat,
                                       String aggOp, File outputFile) {

        //Construct query...
        PreparedStatement ps = null;
        ResultSet rs = null;
        FileOutputStream fout = null;
        byte[] ras = null;
        try {
            //start and end time should be not null and valid dates...
            //withinPolygon should be not null but could be not an area polygon (point, line, etc)

            String query = getSpatioTemporalQuery(table, rasterCol, startTime, endTime, withinPolygon, outputFormat, aggOp);

            //Execute query...
            ps = PG_CON.prepareStatement(query);
            rs = ps.executeQuery();
            //Don't know if it should return all in one row or in multiple ones... For know let it be just one
            //If every row must be united into one to correctly export it then it should be better to implement
            //a stored procedure in postgresql to directly export it to a file (without loading the data in memory).

            //Better for now just return a raster always (Aggregating in case of multiple ones).

            //Do something with the retrieved data...
            if (outputFile != null && rs.next()) {
                //Write to file instead of returning
                fout = new FileOutputStream(outputFile);

                fout.write(rs.getBytes(1));

            } else if (rs.next()) {
                ras = rs.getBytes(1);
            }
        } catch (SQLException e) {
            MessageMediator.sendMessage("Error while retrieving raster data!: " + e,
                    MessageMediator.ERROR_MESSAGE);
        } catch (IOException e) {
            MessageMediator.sendMessage("Error writing raster data to file! :>" + e,
                    MessageMediator.ERROR_MESSAGE);
        } finally {
            try {
                close(ps, rs);
                if(fout != null) fout.close();
            } catch (SQLException | IOException e1) {
                e1.printStackTrace();
            }
        }

        return ras;
    }

    //Get data from point...
    public Number[] getDataFromPoint(String table, String rasterCol, Point point, LocalDateTime startTime,
                                            LocalDateTime endTime) {

        PreparedStatement ps = null;
        ResultSet rs = null;
        Number[] ras = null;

        if (table == null || table.isEmpty() || rasterCol == null || rasterCol.isEmpty() ||
                point == null || point.isEmpty()) {
            MessageMediator.sendMessage("Error! table, rasterCol and point are obligatory!",
                    MessageMediator.ERROR_MESSAGE);
            return null;
        }

        try {
            String query = "SELECT array_agg(val) FROM (SELECT ST_Value(" + rasterCol + ", " +
                    "ST_GeomFromText('" + point + "')) AS val FROM " + table + " WHERE (" + rasterCol +
                    " ~ ST_GeomFromText('" + point + "'))";


            //Just add time restriction too in case of existence...
            if (startTime != null && endTime != null) {
                query += " AND ("+ TIME_COL + " BETWEEN '" + startTime + "' AND '" + endTime + "')";
            } else if (startTime != null) {
                //Only startTime
                query += " AND (" + TIME_COL + " >= '" + startTime + "')";
            } else if (endTime != null) {
                //Only endTime
                query += " AND (" + TIME_COL + " <= '" + endTime + "')";
            }

            query += " ORDER BY " + TIME_COL + ") AS t";

            MessageMediator.sendMessage(query);

            //Execute query...
            ps = PG_CON.prepareStatement(query);
            rs = ps.executeQuery();

            if(rs.next()) {
                //This could be a null or empty array I guess, so execute with caution...
                Array arr = rs.getArray(1);
                if(arr != null) ras = (Number[]) arr.getArray();
            }
        } catch (SQLException e) {
            MessageMediator.sendMessage("Error while retrieving the point data!: " + e,
                    MessageMediator.ERROR_MESSAGE);
        } finally {
            try {
                close(ps, rs);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        return ras;
    }

    public List<Map<String, Object>> getStorageSchema(String database) {
        List<Map<String, Object>> schema = new ArrayList<>();
        Map<String, Object> table = new HashMap<>();

        table.put("table", database);
        try {
            ResultSet rs = PG_CON.getMetaData()
                    .getColumns(null, null, database, null);
            List<Map<String, String>> columns = new ArrayList<>();

            while(rs.next()) {
                Map<String, String> column = new HashMap<>();

                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("type", rs.getString("TYPE_NAME"));

                columns.add(column);
            }
            //Once read the columns, append it to the main "table" so to build the structure: {table, columns: [{name, type}]}
            table.put("columns", columns);

            close(null, rs);
            //finally, append the table (one in this case, but possible to have more than one in other implementations)
            schema.add(table);
        } catch (SQLException e) {
            MessageMediator.sendMessage("Error while retrieving underlying schema columns!: " + e,
                    MessageMediator.ERROR_MESSAGE);
            schema = null;
        }

        return schema;
    }

}