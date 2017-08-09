package mx.uabc.mxl.iing.azul.dataplugin.load;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CopyStrategiesFactory;
import mx.uabc.mxl.iing.azul.dataplugin.datastore.CopyStrategy;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Reader;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.load.PluginLoader;


/**
 * Loader class is in charge of loading a plugin file as a new {@link Plugin} instance, so it can be registered and
 * executed by the application
 *
 * @author jdosornio
 * @version %I%
 */
public class Loader {
    /**
     * Loads a new plugin from a plugin file stored in disk
     *
     * @param pluginFile a File object representing the path of the plugin file on disk to be loaded
     * @return
     */
    public static Plugin load(File pluginFile)
            throws IOException {
    	Plugin plugin;
        URLClassLoader loader = null;
        PluginDescriptor pluginDescriptor = Reader.getPluginDescriptor(pluginFile);

        //must check if plugin descriptor has all obligatory information, otherwise abort load
        if(pluginDescriptor == null) {
            MessageMediator.sendMessage("Plugin Descriptor is Null!!!");
            return null;
        }

        try {
            //Load the class of the given jar
            loader = new URLClassLoader(
                    new URL[]{new URL("jar:file:" + pluginFile + "!/")},
                    Loader.class.getClassLoader());

            //Load the loader and return the PluginInterface for this plugin
            PluginLoader pluginLoader = (PluginLoader) loader
                    .loadClass(pluginDescriptor.getLoaderClass()).newInstance();

            //Get CopyStrategy from factory based on plugin descriptor file
            CopyStrategy copyStrategy = CopyStrategiesFactory
                    .getStrategy(pluginDescriptor.getTargetDB());
            //Use the plugin own loader to load this plugin
            plugin = pluginLoader.load(pluginDescriptor, copyStrategy);
        } catch(InstantiationException | IllegalAccessException |
                ClassNotFoundException ex) {
            plugin = null;
            MessageMediator.sendMessage("Error while loading plugin: " + pluginFile +
                    " loader-class name: " + pluginDescriptor.getLoaderClass() +
                            " Ex: " + ex, MessageMediator.ERROR_MESSAGE);
        } finally {
            if (loader != null) loader.close();
        }
        return plugin;
    }
}