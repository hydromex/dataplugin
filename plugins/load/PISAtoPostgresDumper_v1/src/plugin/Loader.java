package plugin;

import mx.uabc.mxl.iing.azul.dataplugin.datastore.CopyStrategy;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.load.PluginLoader;

import java.net.URL;

/**
 * Created by jdosornio on 28/10/16.
 */
public class Loader implements PluginLoader {

    @Override
    public Plugin load(PluginDescriptor descriptor, CopyStrategy strategy) {
        URL scriptFile = this.getClass().getResource(descriptor.getScripts().get(0));
        URL helpFile = this.getClass().getResource(descriptor.getHelp());

        return new PluginMain(descriptor, strategy, scriptFile, helpFile);
    }
}