package mx.uabc.mxl.iing.azul.dataplugin.plugin;

import mx.uabc.mxl.iing.azul.dataplugin.descriptor.PluginDescriptor;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.util.FileUtil;

import java.io.File;
import java.util.Date;

public abstract class Plugin {

    public String getName() {
        return getDescriptor().getName();
    }

    public String getVersion() {
        return getDescriptor().getVersion();
    }

    public String getDescription() {
        return getDescriptor().getDescription();
    }

    //maybe other return type (collection of values)
    public String getMeta() {
        return "";
    }

    public String getHelp() {
        return getHelpContent();
    }

    //only to access subclass descriptor
    abstract protected PluginDescriptor getDescriptor();

    abstract protected String getHelpContent();

    private File createPluginDir() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        //app dir
        File appDir = new File(tmpDir, "DataPlugin");
        String dirName = getName() + "_" +
                getVersion() + "_" + new Date().getTime();

        File pluginDir = new File(appDir, dirName);
        if (!pluginDir.mkdirs()) pluginDir = null;

        return pluginDir;
    }

	public int execute(String ... args) {
        int res;
        //make temp plugin execution dir and make it available
        File pluginDir = createPluginDir();
        if (pluginDir != null) {
            MessageMediator.sendMessage("Plugin Directory created at: " + pluginDir);
            res = execute(pluginDir, args);
            boolean del = FileUtil.deleteDir(pluginDir);
            MessageMediator.sendMessage("Plugin Directory deleted: " + del);
            MessageMediator.sendMessage("Plugin execution termination result code: " + res);
        } else {
            res = -101;
            MessageMediator.sendMessage("Couldn't create plugin directory!! Err code: " + res);
        }

        return res;
    }

    protected abstract int execute(File pluginDir, String ... args);
}