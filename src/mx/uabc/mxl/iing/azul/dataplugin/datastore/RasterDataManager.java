package mx.uabc.mxl.iing.azul.dataplugin.datastore;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface RasterDataManager {
    Map<String, Object> getMetadata(String database, String variable);
    int getWidth(String database, String variable);
    int getHeight(String database, String variable);
    int getBands(String database, String variable);
    Map<String, Double> getOrigin(String database, String variable);
    Map<String, Double> getSpatialRes(String database, String variable);
    Map<String, Date> getTemporalCoverage(String database);
    long getTotalTimes(String database);
    Duration getTemporalRes(String database);
    byte[] getRasterData(String database, String variable, LocalDateTime startTime,
                         LocalDateTime endTime, Geometry withinPolygon, String outputFormat,
                         String aggOp, File outputFile);
    Double[] getDataFromPoint(String database, String variable, Point point,
                              LocalDateTime startTime, LocalDateTime endTime);
    List<Map<String, Object>> getStorageSchema(String database);
}