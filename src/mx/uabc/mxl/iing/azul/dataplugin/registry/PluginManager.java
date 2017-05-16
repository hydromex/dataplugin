/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.uabc.mxl.iing.azul.dataplugin.registry;

import mx.uabc.mxl.iing.azul.dataplugin.execution.Executor;
import mx.uabc.mxl.iing.azul.dataplugin.load.Loader;
import mx.uabc.mxl.iing.azul.dataplugin.location.Locator;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;

/**
 *
 * @author Alex
 */
public class PluginManager {
    
    //REGISTRO DE LOS PLUGINS
    private static final HashMap<String, Plugin> REGISTRY;

    static {
    	REGISTRY = new HashMap<>();
    	loadPlugins();
    }
    
    private static void registerPlugin(File pluginFile) {
        //Get plugin descriptor file
        try {
            Plugin plugin = Loader.load(pluginFile);

            if (plugin == null) {
                MessageMediator.sendMessage("Plugin is Null!!!");
                return;
            }

            REGISTRY.put(plugin.getName(), plugin);
            MessageMediator.sendMessage("PLUGIN NAME REGISTERED: " + plugin.getName());

        } catch (IOException e) {
            MessageMediator.sendMessage(e.getMessage(), MessageMediator.ERROR_MESSAGE);
        }
    }
    
    /**
     * Limpia el registro de plugins.
     */
    private static void clearPlugins() {
        REGISTRY.clear();
        MessageMediator.sendMessage("Cleared all plugins in registry");
    }
    
    /**
     * Carga todos los plugins de los que se cuente un archivo .XML.
     */
    private static void loadPlugins() {
        MessageMediator.sendMessage("Start loading plugins");
        File[] pluginFiles = Locator.getPluginFiles();
        
        for(File pluginFile : pluginFiles) {
            registerPlugin(pluginFile);
        }

        MessageMediator.sendMessage("Finished loading plugins");
    }
    
    /**
     * Se obtiene el plugin con un nombre dado. En caso de no encontrarlo
     * regresa null.
     * @param pluginName Nombre del plugin a obtener
     * @return Regresa el plugin o nulo en caso de no encontrarlo
     */
    private static Plugin getPlugin(String pluginName) {
        //get plugin from the registry, else from locator...
        MessageMediator.sendMessage("Obtaining plugin " + pluginName);
        return REGISTRY.get(pluginName);
    }

    public static void executePlugin(String pluginName, String ... args) {
        Plugin plugin = getPlugin(pluginName);
        Executor.execute(plugin, args);
    }

    public static List<String> listPlugins() {
        List<String> pluginNames = new ArrayList<>();

        REGISTRY.forEach((name, plugin) -> pluginNames.add(name));

        return pluginNames;
    }

    public static String showInfo(String pluginName) {
        Plugin plugin = getPlugin(pluginName);

        return "name: [" + plugin.getName() + "]\n desc:\n [" + plugin.getDescription() + "]\n " +
                "version: [" + plugin.getVersion() + "]\n help:\n\n " + plugin.getHelp();
    }
}