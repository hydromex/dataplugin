package mx.uabc.mxl.iing.azul.dataplugin.plugin;
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

import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.util.FileUtil;

import java.io.File;
import java.util.Date;


/**
 * Abstract base class representing a Plugin loaded. It implements basic functionality but must be completely implemented
 * once a concrete plugin is developed. All the developed plugins must extend from this class.
 *
 * @author jdosornio
 * @version %I%
 */
public abstract class Plugin {

    /**
     * Gets the plugin name
     *
     * @return the plugin name
     */
    public String getName() {
        return getDescriptor().getName();
    }

    /**
     * Gets the plugin version
     *
     * @return the plugin version
     */
    public String getVersion() {
        return getDescriptor().getVersion();
    }

    /**
     * Gets the plugin description
     *
     * @return the plugin description
     */
    public String getDescription() {
        return getDescriptor().getDescription();
    }


    /**
     * This method was originally meant to return the remaining metadata, but instead returns the
     * {@link PluginDescriptor} associated with this Plugin instance
     *
     * @return the {@link PluginDescriptor} object
     */
    public PluginDescriptor getMetadata() {
        return getDescriptor();
    }

    /**
     * Gets the plugin help content
     *
     * @return the plugin help content
     */
    public String getHelp() {
        return getHelpContent();
    }

    //only to access subclass descriptor

    /**
     * Gets this plugin {@link PluginDescriptor}
     *
     * @return this plugin descriptor
     */
    abstract protected PluginDescriptor getDescriptor();

    /**
     * Gets this plugin help content
     *
     * @return this plugin help content as a String
     */
    abstract protected String getHelpContent();

    /**
     * This is a private method. It is intended to be used exclusively by this class,
     * it creates an unique plugin temporal directory for storing all the needed script files
     * and other related temporary data.
     * This plugin directory is created per execution, so a unique plugin directory will be created per
     * execution even if the same plugin is executed multiple times in the same time frame.
     *
     * @return a File object representing the created temporary plugin directory
     */
    private File createPluginDir() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        //app dir
        File appDir = new File(tmpDir, "DataPlugin");
        String dirName = getName() + "_" +
                getVersion() + "_" + new Date().getTime();

        File pluginDir = new File(appDir, dirName);
        if (!pluginDir.mkdirs()) pluginDir = null;

        return pluginDir;
    }

    /**
     * This method applies the Template Method Pattern for plugin execution. It is the base execution method and
     * the one being called by the upper classes. Implements generic plugin functionality regarding its execution.
     *
     * @param args The argument(s) to be sent to the plugin for execution
     *
     * @return an integer. Either zero meaning a successful execution status or and error code otherwise.
     */
	public int execute(String ... args) {
        int res;
        //make temp plugin execution dir and make it available
        File pluginDir = createPluginDir();
        if (pluginDir != null) {
            MessageMediator.sendMessage("Plugin Directory created at: " + pluginDir);
            res = execute(pluginDir, args);
            boolean del = FileUtil.deleteDir(pluginDir);
            MessageMediator.sendMessage("Plugin Directory deleted: " + del);
            MessageMediator.sendMessage("Plugin execution termination result code: " + res);
        } else {
            res = -101;
            MessageMediator.sendMessage("Couldn't create plugin directory!! Err code: " + res);
        }

        return res;
    }

    /**
     * Concrete plugin execution method implementation. Implements the execution logic
     * specific to each concrete plugin.
     *
     * @param pluginDir a File object representing the allocated plugin directory to store all the temporary files
     *                  for the plugin execution if needed
     * @param args The plugin execution argument(s)
     *
     * @return an integer. Either zero meaning a successful execution status or and error code otherwise.
     */
    protected abstract int execute(File pluginDir, String ... args);
}