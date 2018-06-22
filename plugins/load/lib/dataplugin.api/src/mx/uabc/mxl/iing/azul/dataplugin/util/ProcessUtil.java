package mx.uabc.mxl.iing.azul.dataplugin.util;
/*
    Copyright (C) 2017  Jesús Donaldo Osornio Hernández
    Copyright (C) 2017  Luis Alejandro Herrera León
    Copyright (C) 2017  Gabriel Alejandro López Morteo

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

import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utility class for working with system processes
 *
 * @author jdosornio
 * @version %I%
 */
public class ProcessUtil {

    /**
     * Utility method to execute a system process given a command and optional arguments.
     *
     * @param command An array of command and optional arguments to be executed in the system
     *
     * @return true in case of successful execution, false otherwise
     *
     * @throws IOException in case of an error while executing the process
     * @throws InterruptedException in case of the process being interrupted
     */
    public static boolean executeProcess(String ... command) throws IOException,
            InterruptedException {
        if (command == null || command.length == 0) {
            MessageMediator.sendMessage("command is empty!", MessageMediator.ERROR_MESSAGE);
            return false;
        }

        Process process = new ProcessBuilder().redirectErrorStream(true).command(command).start();

        BufferedReader processOutput = new BufferedReader(new InputStreamReader(
                process.getInputStream()));

        //Send lines to message mediator while executing (must write the output in real time but it doesnt)
        String outputLine;
        while ((outputLine = processOutput.readLine()) != null) {
            MessageMediator.sendMessage(outputLine);
        }
        //close opened stream just in case
        processOutput.close();

        //here either error or success, but process already finished (maybe) and streams closed (maybe)
        int res = process.waitFor();

        return (res == 0);
    }

//    public static String getProcessOutput(String ... command) throws IOException, InterruptedException {
//        String procOut;
//
//        if (command == null || command.length == 0) {
//            MessageMediator.sendMessage("command is empty!", MessageMediator.ERROR_MESSAGE);
//            return null;
//        }
//
//        Process process = new ProcessBuilder().command(command).start();
//
//        procOut = FileUtil.streamToString(process.getInputStream());
//
//        int res = process.waitFor();
//
//        if(res != 0) {
//            String error = FileUtil.streamToString(process.getErrorStream());
//            MessageMediator.sendMessage("error output:\n" + error, MessageMediator.ERROR_MESSAGE);
//        }
//
//        return procOut;
//    }
}