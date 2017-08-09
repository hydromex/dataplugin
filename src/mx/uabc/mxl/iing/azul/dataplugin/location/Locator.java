package mx.uabc.mxl.iing.azul.dataplugin.location;
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


import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Reader;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;

import java.io.File;
import java.util.Arrays;

/**
 * Locators function is to locate the available plugin files for the application to load.
 *
 * @author jdosornio
 * @version %I%
 */
public class Locator {
    private static final File PLUGIN_DIR = new File(Reader.getConfiguration().getPluginDir());

    /**
     * Gets all the plugin files stored in the application plugin directory.
     *
     * @return an array of the plugin files available in the application plugin directory
     */
    public static File[] getPluginFiles() {
        File[] result;
        MessageMediator.sendMessage("Getting plugin files from: " + PLUGIN_DIR.getAbsolutePath());
        result =  PLUGIN_DIR.listFiles((File dir1, String filename) -> filename.endsWith(".plugin"));
        MessageMediator.sendMessage("All plugin files obtained\n"
                + Arrays.toString(result));
        return result;
    }
}