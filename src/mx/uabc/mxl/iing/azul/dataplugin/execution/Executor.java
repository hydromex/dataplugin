package mx.uabc.mxl.iing.azul.dataplugin.execution;
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
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;

import java.util.Arrays;

/**
 * The function of Executor is manage and control all the processes related to executing a given plugin.
 *
 * @author jdosornio
 * @version %I%
 */
public class Executor {

    //TODO: create better async execution. With futures or Executor framework or even JavaRX

    /**
     * Executes a given plugin with the given arguments
     *
     * @param plugin a plugin instance to be executed
     * @param args the parameter(s) to be sent to the plugin as arguments for its execution
     */
    public static void execute(Plugin plugin, String ... args) {
        //Execute logic, security, sandbox, concurrency...
        new Thread(() -> {
            MessageMediator.sendMessage("Executing plugin: " + plugin.getName());
            plugin.execute(args);
            MessageMediator.sendMessage(plugin.getName() + " plugin execution finalized...");
        }).start();
        MessageMediator.sendMessage("Executing plugin [" + plugin.getName() + "] with args [" +
                Arrays.toString(args) + "] in a new thread...");
    }
}