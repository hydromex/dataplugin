package mx.uabc.mxl.iing.azul.dataplugin.location;

import mx.uabc.mxl.iing.azul.dataplugin.descriptor.Reader;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;

import java.io.File;
import java.util.Arrays;

/**
 * Created by jdosornio on 23/04/17.
 */
public class Locator {
    private static final File PLUGIN_DIR = new File(Reader.getConfiguration().getPluginDir());

    /**
     * Obtiene todos los archivos plugin del directorio de plugins. Estos archivos
     * son los empaquetamientos de los plugins, asi se podra obtener los
     * plugins que ya hayan sido registrados.
     * @return Regresa el arreglo de archivos de plugin
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