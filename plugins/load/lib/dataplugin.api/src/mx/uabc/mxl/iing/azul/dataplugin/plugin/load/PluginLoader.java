package mx.uabc.mxl.iing.azul.dataplugin.plugin.load;
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

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CopyStrategy;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;

/**
 * Plugin interface with the sole purpose of defining the method to load a new
 * plugin instance from a given {@link PluginDescriptor} object. Must be implemented
 * by each and every plugin being developed
 *
 * @author jdosornio
 * @version %I%
 */
public interface PluginLoader {
	
	/**
	 * Loads the classes needed for a plugin to run.
	 * 
	 * @return a {@link Plugin} implementation with the new plugin loaded
	 */
	Plugin load(PluginDescriptor descriptor, CopyStrategy strategy);
}