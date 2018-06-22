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

import java.util.List;
import java.util.Map;


/**
 * Class representing an instance of a plugin descriptor file
 *
 * @author jdosornio
 * @version %I%
 */
public class PluginDescriptor {
    private final Map<String, Object> DESCRIPTOR;

    private static final String PLUGIN_KEY = "plugin";
    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";
    private static final String DESCRIPTION_KEY = "description";
    private static final String HELP_KEY = "help-file";
    private static final String LOADER_CLASS_KEY = "loader-class";
    private static final String SCRIPT_KEY = "script";
    private static final String TARGET_DB_KEY = "target-DB";


    /**
     * Create the PluginDescriptor with the given map
     *
     * @param desc the map object
     */
    public PluginDescriptor(Map<String, Object> desc) {
        //later validation of obligatory fields
        DESCRIPTOR = desc;
    }

    /**
     * Shortcut method for getting the plugin root from the descriptor
     *
     * @return a map object representing the plugin root
     */
    private Map<String, Object> getPluginRoot() {
        return (Map)DESCRIPTOR.get(PLUGIN_KEY);
    }

    /**
     * Get the plugin name
     *
     * @return the name string
     */
    public String getName() {
        return getPluginRoot().get(NAME_KEY).toString();
    }

    /**
     * Get the plugin version
     *
     * @return the version string
     */
    public String getVersion() {
        return getPluginRoot().get(VERSION_KEY).toString();
    }

    /**
     * Get this plugin description text
     *
     * @return the description string
     */
    public String getDescription() {
        return getPluginRoot().get(DESCRIPTION_KEY).toString();
    }

    /**
     * Get the plugin help file path
     *
     * @return the path as a string
     */
    public String getHelp() {
        return getPluginRoot().get(HELP_KEY).toString();
    }

    /**
     * Get the full qualified name of this plugin loader class
     *
     * @return class name as a string
     */
    public String getLoaderClass() {
        return getPluginRoot().get(LOADER_CLASS_KEY).toString();
    }

    /**
     * Get a list with the plugin script(s) path(s)
     *
     * @return list of strings
     */
    public List<String> getScripts() {
        return (List)getPluginRoot().get(SCRIPT_KEY);
    }

    /**
     * Get the name of the DBMS that this plugins loads data into
     *
     * @return the name of the DBMS
     */
    public String getTargetDB() {
        return getPluginRoot().get(TARGET_DB_KEY).toString();
    }

    /**
     * Get this PluginDescriptor metadata as a map object
     *
     * @return a map object
     */
    public Map<String, Object> asMap() {
        return getPluginRoot();
    }
}