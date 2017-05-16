package mx.uabc.mxl.iing.azul.dataplugin.execution;


import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;

import java.util.Arrays;

/**
 * Created by jdosornio on 30/10/16.
 */
public class Executor {

    //TODO: create better async execution. With futures or Executor framework or even JavaRX
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