package mx.uabc.mxl.iing.azul.dataplugin.descriptor;

import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

/**
 * Created by jdosornio on 29/10/16.
 */
public class Reader {
    private static Configuration config;
    private static final Yaml YAML = new Yaml();
    private static final String CONFIG_FILE = "/config.yaml";

    //TODO: Create reader for XML descriptors too
    public static PluginDescriptor getPluginDescriptor(File pluginFile) throws IOException {
        final String PLUGIN_DESCRIPTOR_FILE = "desc.yaml";
        PluginDescriptor pluginDesc;
        URLClassLoader loader = null;

        try {
            loader = new URLClassLoader(new URL[]{new URL("jar:file:" +
                    pluginFile + "!/")}, Reader.class.getClassLoader());
            InputStream descFile = loader.getResourceAsStream(PLUGIN_DESCRIPTOR_FILE);
            pluginDesc = new PluginDescriptor((Map) YAML.load(descFile));
        } catch (Exception e) {
            pluginDesc = null;
            MessageMediator.sendMessage(e.getMessage(), MessageMediator.ERROR_MESSAGE);
        } finally {
            //close loader
            if (loader != null) loader.close();
        }

        return pluginDesc;
    }

    public static Configuration getConfiguration() {
        if (config == null) {
            config = new Configuration((Map)YAML.load(Reader.class
                    .getResourceAsStream(CONFIG_FILE)));
        }

        return config;
    }

}