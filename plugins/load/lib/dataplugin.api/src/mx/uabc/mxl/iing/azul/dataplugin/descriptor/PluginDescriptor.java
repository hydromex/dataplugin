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
    private final Map DESCRIPTOR;

    private static final String PLUGIN_KEY = "plugin";
    private static final String NAME_KEY = "name";
    private static final String VERSION_KEY = "version";
    private static final String DESCRIPTION_KEY = "description";
    private static final String HELP_KEY = "help-file";
    private static final String LOADER_CLASS_KEY = "loader-class";
    private static final String SCRIPT_KEY = "script";
    private static final String TARGET_DB_KEY = "target-DB";


    public PluginDescriptor(Map desc) {
        //later validation of obligatory fields
        DESCRIPTOR = desc;
    }

    private Map getPluginRoot() {
        return (Map)DESCRIPTOR.get(PLUGIN_KEY);
    }

    public String getName() {
        return getPluginRoot().get(NAME_KEY).toString();
    }

    public String getVersion() {
        return getPluginRoot().get(VERSION_KEY).toString();
    }

    public String getDescription() {
        return getPluginRoot().get(DESCRIPTION_KEY).toString();
    }

    public String getHelp() {
        return getPluginRoot().get(HELP_KEY).toString();
    }

    public String getLoaderClass() {
        return getPluginRoot().get(LOADER_CLASS_KEY).toString();
    }

    public List<String> getScripts() {
        return (List)getPluginRoot().get(SCRIPT_KEY);
    }

    public String getTargetDB() {
        return getPluginRoot().get(TARGET_DB_KEY).toString();
    }
}