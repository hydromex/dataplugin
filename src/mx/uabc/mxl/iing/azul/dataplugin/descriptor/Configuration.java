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

import java.util.Map;

/**
 * Class representing an instance of the applications configuration file
 *
 * @author jdosornio
 * @version %I%
 */
public class Configuration {
    private final Map CONFIG_ROOT;
    private static final String CONFIG_KEY = "config";
    private static final String PLUGIN_DIR = "plugin-dir";

    Configuration(Map config) {
        CONFIG_ROOT = (Map) config.get(CONFIG_KEY);
    }


    public String getPluginDir() {
        return CONFIG_ROOT.get(PLUGIN_DIR).toString();
    }

}