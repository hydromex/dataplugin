package mx.uabc.mxl.iing.azul.dataplugin.datastore;

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

    PostgisRasterDAO() {

    }

    private static void close(PreparedStatement ps, ResultSet rs) throws SQLException {
        if (ps != null) {
            ps.close();
        }
        if (rs != null) {
            rs.close();
        }
    }

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

        //Later validate if valid aggregate operation...
        aggOp = (aggOp != null && !aggOp.isEmpty()) ? ", '" + aggOp + "'" : "";

        String baseQuery = "SELECT ST_Union(" + rasterCol + ") FROM " + table /*+ " GROUP BY " + TIME_COL +
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
        //If polygon object would be already validated that it's indeed a polygon, but here it's just a string
        //Not gonna add a whole library just for one data type in this method call, maybe in the future when it's needed
        //in other places besides from here
        if (withinPolygon != null && !withinPolygon.isEmpty()) {
            String polygon = "ST_GeomFromText('" + withinPolygon + "')";

            //modify query to clip polygon area from raster
            baseQuery = baseQuery.replaceAll("ST_Union\\((\\S+)\\)",
                    "ST_Union(ST_Clip($1, " + polygon + ")" + aggOp + ")");
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


        System.out.println(baseQuery);

        return baseQuery;
    }

    public byte[] getRasterData(String table, String rasterCol, LocalDateTime startTime,
                                     LocalDateTime endTime, Geometry withinPolygon, String outputFormat,
                                       String aggOp, File outputFile) {

        //Construct query...
        try {
            String query = getSpatioTemporalQuery(table, rasterCol, startTime, endTime, withinPolygon, outputFormat, aggOp);

            //Execute query...
            PreparedStatement ps = PG_CON.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            //Don't know if it should return all in one row or in multiple ones... For know let it be just one
            //If every row must be united into one to correctly export it then it should be better to implement
            //a stored procedure in postgresql to directly export it to a file (without loading the data in memory).

            //Better for now just return a raster always (Aggregating in case of multiple ones).

            //Do something with the retrieved data...
            if (outputFile != null && rs.next()) {
                //Write to file instead of returning
                FileOutputStream fout = new FileOutputStream(outputFile);

                fout.write(rs.getBytes(1));
                fout.close();

                close(ps, rs);
            } else if (rs.next()) {
                byte[] ras = rs.getBytes(1);

                close(ps, rs);

                return ras;
            }
        } catch (SQLException e) {
            MessageMediator.sendMessage("Error while retrieving raster data!: " + e,
                    MessageMediator.ERROR_MESSAGE);
        } catch (IOException e) {
            MessageMediator.sendMessage("Error writing raster data to file! :>" + e,
                    MessageMediator.ERROR_MESSAGE);
        }

        return null;
    }

    //Get data from point...
    public Double[] getDataFromPoint(String table, String rasterCol, Point point, LocalDateTime startTime,
                                            LocalDateTime endTime) {

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

            System.out.println(query);

            //Execute query...
            PreparedStatement ps = PG_CON.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                Double[] ras = (Double[]) rs.getArray(1).getArray();

                close(ps, rs);

                return ras;
            }

            close(ps, rs);
        } catch (SQLException e) {
            MessageMediator.sendMessage("Error while retrieving the point data!: " + e,
                    MessageMediator.ERROR_MESSAGE);
        }

        return null;
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



//    public static void main(String[] args) {
//        try {
//            System.out.println(PostgisRasterDAO.getMetadata("trmm", "precipitation"));
//            System.out.println("Longitudes: " + PostgisRasterDAO.getWidth("trmm", "precipitation"));
//            System.out.println("Latitudes: " + PostgisRasterDAO.getHeight("trmm", "precipitation"));
//            System.out.println("Temp. coverage: " + PostgisRasterDAO.getTemporalCoverage("trmm"));
//            System.out.println("Total times: " + PostgisRasterDAO.getTotalTimes("trmm"));
//            System.out.println("Height levels: " + PostgisRasterDAO.getBands("trmm", "precipitation"));
//            System.out.println("Origin: " + PostgisRasterDAO.getOrigin("trmm", "precipitation"));
//            System.out.println("Spatial resolution: " + PostgisRasterDAO.getSpatialRes("trmm", "precipitation"));
//            System.out.println("Temporal resolution: " + PostgisRasterDAO.getTemporalRes("trmm"));
//            System.out.println(Arrays.toString(PostgisRasterDAO.getRasterData("trmm",
//                    "precipitation", LocalDateTime.of(2011, 10, 5, 0, 0),
//                    LocalDateTime.of(2011, 10, 10, 0, 0),
//                    new GeometryFactory().toGeometry(new Envelope(new Coordinate(-125.746697, 49.484329),
//                            new Coordinate(-66.359532, 23.378081))),
//                    "GTiff", "MEAN", new File("/home/jdosornio/raster"))));
//            System.out.println(Arrays.toString(PostgisRasterDAO.getDataFromPoint("trmm", "precipitation",
//                    new GeometryFactory().createPoint(new Coordinate(-160, 45.75)),
//                    LocalDateTime.of(2011, 7, 7, 0, 0),
//                    LocalDateTime.of(2011, 7, 9, 0, 0))));
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//
//    }

}