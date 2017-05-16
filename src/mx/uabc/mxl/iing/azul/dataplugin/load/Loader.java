package mx.uabc.mxl.iing.azul.dataplugin.load;

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

public class Loader {
    /**
     * Carga un plugin mediante un archivo de descripción
     * @param pluginFile
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