package mx.uabc.mxl.iing.azul.dataplugin.plugin.load;

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CopyStrategy;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;

public interface PluginLoader {
	
	/**
	 * Loads the classes needed for a plugin to run.
	 * 
	 * @return a PluginInterface implementation with the new plugin loaded
	 */
	Plugin load(PluginDescriptor descriptor, CopyStrategy strategy);
}