package mx.uabc.mxl.iing.azul.dataplugin.service;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jdosornio on 4/05/17.
 */
@ApplicationPath("/rs")
public class RestApp extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(PluginService.class));
    }
}