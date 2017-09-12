package mx.uabc.mxl.iing.azul.dataplugin.descriptor;
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
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

/**
 * Reader is the class in charge of loading the contents of configuration and descriptor files in memory.
 * It can read the main application configuration file as well as any plugin descriptor file
 *
 * @author jdosornio
 * @version %I%
 */
public class Reader {
    private static Configuration config;
    private static final Yaml YAML = new Yaml();
    //Should be read from an arbitrary path to avoid having to redeploy the application each time the configuration is
    //changed
    private static final String CONFIG_FILE = "/config.yaml";

    /**
     * This method reads the descriptor file of the given plugin file and loads it into memory.
     *
     * @param pluginFile File object representing the path of the plugin file or null in case of an error happening
     *
     * @return A {@link PluginDescriptor} instance representing the plugin descriptor read
     *
     * @throws IOException in case of an error happening while reading the plugin file or its descriptor.
     */
    public static PluginDescriptor getPluginDescriptor(File pluginFile) throws IOException {
        final String PLUGIN_DESCRIPTOR_FILE = "desc.yaml";
        PluginDescriptor pluginDesc;
        URLClassLoader loader = null;

        try {
            loader = new URLClassLoader(new URL[]{new URL("jar:file:" +
                    pluginFile + "!/")}, Reader.class.getClassLoader());
            InputStream descFile = loader.getResourceAsStream(PLUGIN_DESCRIPTOR_FILE);
            pluginDesc = new PluginDescriptor((Map) YAML.load(descFile));
        } catch (Exception e) {
            pluginDesc = null;
            MessageMediator.sendMessage(e.getMessage(), MessageMediator.ERROR_MESSAGE);
        } finally {
            //close loader
            if (loader != null) loader.close();
        }

        return pluginDesc;
    }

    //TODO: Create reader for XML descriptors too

    /**
     * Returns an instance of the main application configuration file
     *
     * @return A {@link Configuration} instance of the main application configuration file.
     */
    public static Configuration getConfiguration() {
        if (config == null) {
            config = new Configuration((Map)YAML.load(Reader.class
                    .getResourceAsStream(CONFIG_FILE)));
        }

        return config;
    }

}