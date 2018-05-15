package plugin;

import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;
import mx.uabc.mxl.iing.azul.dataplugin.util.FileUtil;
import mx.uabc.mxl.iing.azul.dataplugin.util.ProcessUtil;
import mx.uabc.mxl.iing.azul.dataplugin.util.RestUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdosornio on 28/10/16.
 */
public class PluginMain extends Plugin {

    private final PluginDescriptor DESCRIPTOR;
    private final URL DUMP_SCRIPT;
    private final URL LOAD_SCRIPT;
    private final URL NC_LIB;
    private final URL HELP;

    private String helpContent;


    PluginMain(PluginDescriptor descriptor, URL ... files) {
        DESCRIPTOR = descriptor;
        DUMP_SCRIPT = files[0];
        LOAD_SCRIPT = files[1];
        NC_LIB = files[2];
        HELP = files[3];
    }

    @Override
    protected PluginDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    protected String getHelpContent() {
        if (helpContent != null) {
            return helpContent;
        }
        try {
            helpContent = FileUtil.streamToString(HELP.openStream());
        } catch (IOException e) {
            sendError("error reading help content");
            helpContent = null;
        }

        return helpContent;
    }

    @Override
    protected int execute(File pluginDir, String ... args) {

        if(args == null || args.length != 4) {
            sendInfo("Incorrect usage, help:\n" + getHelp());
            return -1;
        }

        final String PYTHON_COMMAND = "python3";
        //python dumper script arguments
        final String INPUT_DATA = args[0];
        final String DUMP_VAR = args[1];
        final String ST_DATE = args[2];
        final String ED_DATE = args[3];

        final String BASH_COMMAND = "bash";
        //bash loader script arguments
        final String GIS_DB = "griddb";
        final String RASTER_TABLE = "trmm";
        final String TILE_WIDTH = "30";
        final String TILE_HEIGHT = "30";

        final String CATALOG_URL = "http://localhost:8080/dataplugin/rs/meta/register-grid-db";

        File dumpedNCFile = new File(pluginDir, "dumpedData.nc");
        int res = 0;

        try {
            //Measure total execution time
            Instant dumpStart = Instant.now();

            File[] scriptFiles = FileUtil.copyFilesTo(pluginDir, DUMP_SCRIPT, LOAD_SCRIPT, NC_LIB);
            sendInfo("Script files created at: " + Arrays.toString(scriptFiles));

            //execute script to agregate netCDF files to one netCDF file
            //happy path
            Instant startTime = Instant.now();
            boolean ok = ProcessUtil.executeProcess(PYTHON_COMMAND, scriptFiles[0].getPath(),
                    "--stDate", ST_DATE, "--edDate", ED_DATE, "-f", "nc", INPUT_DATA,
                    DUMP_VAR, "-o", dumpedNCFile.getPath());
            sendInfo("NetCDF dump file creation total execution time: "
                    + Duration.between(startTime, Instant.now()));

            if(ok) {
                //netCDF dump successfully executed, time to load the aggregated netCDF file to PostgreSQL
                startTime = Instant.now();

                ok = ProcessUtil.executeProcess(BASH_COMMAND, scriptFiles[1].getPath(), dumpedNCFile.getPath(),
                        GIS_DB, RASTER_TABLE, DUMP_VAR, TILE_WIDTH, TILE_HEIGHT);
                sendInfo("Database load total execution time: "
                        + Duration.between(startTime, Instant.now()));

                if(ok) {
                    //after making sure the data is saved finally update the catalog metadata database
                    Map<String, String> header = new HashMap<>();
                    Map<String, Object> fields = new HashMap<>();

                    header.put("accept", "text/plain");
                    fields.put("db-name", RASTER_TABLE);
                    fields.put("plugin-name", getName());
                    fields.put("plugin-version", getVersion());
                    fields.put("variables", DUMP_VAR);
                    fields.put("dbms", getDescriptor().getTargetDB());

                    sendInfo(RestUtil.post(CATALOG_URL, header, fields, String.class));
                } else {
                    sendError("Error executing PostgreSQL import process!!");
                    res = -3;
                }

            } else {
                sendError("Error executing python3 process!!");
                res = -2;
            }
            sendInfo("Import process total execution time: "
                    + Duration.between(dumpStart, Instant.now()));

        } catch (IOException | InterruptedException e) {
            sendError(e.toString());
            res = -4;
        }

        return res;

    }

    private void sendInfo(String message) {
        MessageMediator.sendMessage(getName(), message, MessageMediator.INFO_MESSAGE);
    }

    private void sendError(String message) {
        MessageMediator.sendMessage(getName(), message, MessageMediator.ERROR_MESSAGE);
    }
}