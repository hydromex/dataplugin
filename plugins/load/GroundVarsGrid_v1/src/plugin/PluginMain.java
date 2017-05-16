package plugin;

import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;
import mx.uabc.mxl.iing.azul.dataplugin.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;

/**
 * Created by jdosornio on 28/10/16.
 */
public class PluginMain extends Plugin {

    private final PluginDescriptor DESCRIPTOR;
    private final URL SCRIPT;
    private final URL HELP;


    public PluginMain(PluginDescriptor descriptor, URL script, URL help) {
        DESCRIPTOR = descriptor;
        SCRIPT = script;
        HELP = help;
    }

    @Override
    protected PluginDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    protected String getHelpContent() {
        String help;

        try {
            help = FileUtil.streamToString(HELP.openStream());
        } catch (IOException e) {
            MessageMediator.sendMessage(getName(), "error reading help content",
                    MessageMediator.ERROR_MESSAGE);
            help = null;
        }

        return help;
    }

    private boolean executeProcess(String ... command) throws IOException,
            InterruptedException {
        if (command == null || command.length == 0) {
            MessageMediator.sendMessage("command is empty!", MessageMediator.ERROR_MESSAGE);
            return false;
        }

        Process process = new ProcessBuilder().command(command).start();

        BufferedReader processOutput = new BufferedReader(new InputStreamReader(
                process.getInputStream()));

        //Send lines to message mediator while executing (must write the output in real time but it doesnt)
        String outputLine;
        while ((outputLine = processOutput.readLine()) != null) {
            MessageMediator.sendMessage(outputLine, MessageMediator.INFO_MESSAGE);
        }
        //close opened stream just in case
        processOutput.close();

        //here either error or success, but process already finished (maybe) and streams closed (maybe)
        int res = process.waitFor();

        if(res != 0) {
            String error = FileUtil.streamToString(process.getErrorStream());
            MessageMediator.sendMessage(getName(), "error output:\n" + error, MessageMediator.ERROR_MESSAGE);
        }

        return (res == 0);
    }

    @Override
    protected void execute(File pluginDir, String ... args) {
        final String PYTHON_COMMAND = "python";
        final String CQLSH_COMMAND = "cqlsh";

        File outputCSV = new File(pluginDir, "out.csv");

        if(args == null || args.length != 1) {
            MessageMediator.sendMessage(getName(), "Incorrect usage, help:\n"
                    + getHelpContent(), MessageMediator.INFO_MESSAGE);
            return;
        }

        try {
            MessageMediator.sendMessage(getName(), "Executing Plugin ...",
                    MessageMediator.INFO_MESSAGE);
            File scriptFile = FileUtil.copyFilesTo(pluginDir, SCRIPT)[0];
            MessageMediator.sendMessage(getName(), "Script file created at: " + scriptFile,
                    MessageMediator.INFO_MESSAGE);

            //execute script to dump data to csv
            //happy path
            boolean ok = executeProcess(PYTHON_COMMAND, scriptFile.getPath(), args[0],
                    outputCSV.getPath());

            if(ok) {
                //csv dump successfully executed, time to load it into cassandra with cqlsh command
                //Generate cql instruction file
                String CQL_STMT = "COPY groundvarsgrid.variables FROM '"
                        + outputCSV.getPath() + "' WITH HEADER = TRUE;";
                File cqlFile = new File(pluginDir, "load.cql");
                //write cql file
                Files.write(cqlFile.toPath(), CQL_STMT.getBytes());

                executeProcess(CQLSH_COMMAND, "-f", cqlFile.getPath());
            }

        } catch (IOException | InterruptedException e) {
            MessageMediator.sendMessage(getName(), e.toString(), MessageMediator.ERROR_MESSAGE);
        }

    }
}