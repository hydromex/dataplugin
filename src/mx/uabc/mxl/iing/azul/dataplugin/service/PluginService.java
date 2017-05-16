package mx.uabc.mxl.iing.azul.dataplugin.service;

import mx.uabc.mxl.iing.azul.dataplugin.registry.PluginManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by jdosornio on 4/05/17.
 */
@Path("/p")
public class PluginService {

    @Path("exec")
    // The Java method will process HTTP GET requests
    @POST
    // The Java method will produce content identified by the MIME Media type "text/plain"
//    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String executePlugin(@FormParam("name") String pluginName, @FormParam("args") List<String> args) {
        PluginManager.executePlugin(pluginName, args.toArray(new String[args.size()]));

        return "Plugin [" + pluginName + "] sent with args [" + args + "] for async execution...";
    }

    @Path("list")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String listPlugins() {
        return PluginManager.listPlugins().toString();
    }

    @Path("desc/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String showPluginInfo(@PathParam("name") String pluginName) {
        return PluginManager.showInfo(pluginName);
    }
}