package plugin;

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CopyStrategy;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;
import mx.uabc.mxl.iing.azul.dataplugin.util.FileUtil;
import mx.uabc.mxl.iing.azul.dataplugin.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

/**
 * Created by jdosornio on 28/10/16.
 */
public class PluginMain extends Plugin {

    private final PluginDescriptor DESCRIPTOR;
    private final URL SCRIPT;
    private final URL HELP;
    private final CopyStrategy COPY_STRATEGY;

    private String helpContent;


     public PluginMain(PluginDescriptor descriptor, CopyStrategy strategy, URL ... files) {
        DESCRIPTOR = descriptor;
        SCRIPT = files[0];
        HELP = files[1];
        COPY_STRATEGY = strategy;
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
            MessageMediator.sendMessage(getName(), "error reading help content",
                    MessageMediator.ERROR_MESSAGE);
            helpContent = null;
        }

        return helpContent;
    }

    @Override
    protected int execute(File pluginDir, String ... args) {
        final String PYTHON_COMMAND = "python3";
        final String PSQL_COMMAND = "psql";

        File outputDir = new File(pluginDir, "PISA_DATA");
        int res = 0;

        if(args == null || args.length != 1) {
            MessageMediator.sendMessage(getName(), "Incorrect usage, help:\n"
                    + getHelp(), MessageMediator.INFO_MESSAGE);
            return -1;
        }

        try {
            //Measure total execution time
            Instant dumpStart = Instant.now();

            MessageMediator.sendMessage(getName(), "Executing Plugin ...",
                    MessageMediator.INFO_MESSAGE);
            File scriptFile = FileUtil.copyFilesTo(pluginDir, SCRIPT)[0];
            MessageMediator.sendMessage(getName(), "Script file created at: " + scriptFile,
                    MessageMediator.INFO_MESSAGE);

            //execute script to dump data to csv
            //happy path
            Instant startTime = Instant.now();
            boolean ok = ProcessUtil.executeProcess(PYTHON_COMMAND, scriptFile.getPath(), args[0],
                    outputDir.getPath());
            MessageMediator.sendMessage(getName(), "CSV files creation total execution time: "
                    + Duration.between(startTime, Instant.now()), MessageMediator.INFO_MESSAGE);

            if(ok) {
                //csv dump successfully executed, time to load it into postgresql with the generated sql file
                startTime = Instant.now();
                File sqlFile = new File(outputDir, "loadDatabases.sql");
                ProcessUtil.executeProcess(PSQL_COMMAND, "-U", "postgres", "-h", "localhost", "-f", sqlFile.getPath());
                MessageMediator.sendMessage(getName(), "Database load total execution time: "
                        + Duration.between(startTime, Instant.now()), MessageMediator.INFO_MESSAGE);
            } else {
                MessageMediator.sendMessage(getName(), "Error executing python3 process!!",
                        MessageMediator.ERROR_MESSAGE);
                res = -2;
            }
            MessageMediator.sendMessage(getName(), "Dump process total execution time: "
                    + Duration.between(dumpStart, Instant.now()), MessageMediator.INFO_MESSAGE);
        } catch (IOException | InterruptedException e) {
            MessageMediator.sendMessage(getName(), e.toString(), MessageMediator.ERROR_MESSAGE);
            res = -3;
        }

        return res;

    }
}