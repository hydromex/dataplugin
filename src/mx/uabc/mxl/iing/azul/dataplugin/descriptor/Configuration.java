package mx.uabc.mxl.iing.azul.dataplugin.descriptor;

import java.util.Map;

/**
 * Created by jdosornio on 7/05/17.
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