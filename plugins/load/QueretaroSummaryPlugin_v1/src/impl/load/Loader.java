package impl.load;


import impl.main.PluginImpl;
import mx.uabc.mxl.iing.azul.dataplugin.datastore.CopyStrategy;
import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.Plugin;
import mx.uabc.mxl.iing.azul.dataplugin.plugin.load.PluginLoader;

import java.net.URL;

public class Loader implements PluginLoader {


    @Override
    public Plugin load(PluginDescriptor descriptor, CopyStrategy strategy) {
        URL scriptFile = this.getClass().getResource(descriptor.getScripts().get(0));
        URL helpFile = this.getClass().getResource(descriptor.getHelp());

        return new PluginImpl(descriptor, strategy, scriptFile, helpFile);
    }
}