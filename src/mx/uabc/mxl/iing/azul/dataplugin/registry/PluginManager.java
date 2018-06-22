package mx.uabc.mxl.iing.azul.dataplugin.registry;
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

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CatalogManager;
import mx.uabc.mxl.iing.azul.dataplugin.execution.Executor;
import mx.uabc.mxl.iing.azul.dataplugin.load.Loader;
import mx.uabc.mxl.iing.azul.dataplugin.location.Locator;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;

/**
 * PluginManager is the core class of the DataPlugin application. It is in charge of managing all the plugins available
 * to be executed. The PluginManager functions range from registering new plugins and getting their metadata to executing
 * them (through delegation to {@link Executor})
 *
 * @author jdosornio
 * @author aherrera
 * @version %I%
 */
public class PluginManager {
    
    //PLUGINS REGISTRY
    private static final HashMap<String, Plugin> REGISTRY;

    static {
    	REGISTRY = new HashMap<>();
    	loadPlugins();
    }

    /**
     * Loads a new {@link Plugin} instance from a file and registers it so it can be executed on request
     *
     * @param pluginFile the plugin file to be loaded
     */
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
     * Clears plugin registry
     */
    private static void clearPlugins() {
        REGISTRY.clear();
        MessageMediator.sendMessage("Cleared all plugins in registry");
    }
    
    /**
     * Loads all available plugin files in the application plugin directory.
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
     * Gets the plugin with the given name.
     *
     * @param pluginName Name of the plugin to get
     * @return returns the plugin instance with the given name, or null in case not found
     */
    private static Plugin getPlugin(String pluginName) {
        //get plugin from the registry, else from locator...
        MessageMediator.sendMessage("Obtaining plugin " + pluginName);
        return REGISTRY.get(pluginName);
    }

    /**
     * Executes the plugin with the given name and sends the given argument(s) to it for execution
     *
     * @param pluginName the name of the plugin to be executed
     * @param args the parameter(s) to be sent to the plugin as arguments for its execution
     */
    public static void executePlugin(String pluginName, String ... args) {
        Plugin plugin = getPlugin(pluginName);
        Executor.execute(plugin, args);
        //Register plugin in the catalog, even if the execution is not finished
        CatalogManager.registerPlugin(plugin);
    }

    /**
     * Gets the name and version of all the registered plugins in the PluginManager
     *
     * @return a list of maps containing the registered plugin names and versions as [{name, version}]
     */
    public static List<Map<String, String>> listPlugins() {
        List<Map<String, String>> plugins = new ArrayList<>();

        REGISTRY.forEach((name, plugin) -> {
            Map<String, String> p = new HashMap<>();

            p.put("name", name);
            p.put("version", plugin.getVersion());

            plugins.add(p);
        });

        return plugins;
    }

    /**
     * Gets the metadata of a specific plugin given its name
     *
     * @param pluginName the name of the plugin
     * @return The requested plugin metadata as a String
     */
    public static String showInfo(String pluginName) {
        Plugin plugin = getPlugin(pluginName);

        return "name: [" + plugin.getName() + "]\n desc:\n [" + plugin.getDescription() + "]\n " +
                "version: [" + plugin.getVersion() + "]\n help:\n\n " + plugin.getHelp();
    }
}