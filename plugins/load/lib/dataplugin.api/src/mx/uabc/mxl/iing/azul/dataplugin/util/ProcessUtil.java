package mx.uabc.mxl.iing.azul.dataplugin.util;

import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by jdosornio on 14/05/17.
 */
public class ProcessUtil {

    public static boolean executeProcess(String ... command) throws IOException,
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
            MessageMediator.sendMessage("error output:\n" + error, MessageMediator.ERROR_MESSAGE);
        }

        return (res == 0);
    }
}