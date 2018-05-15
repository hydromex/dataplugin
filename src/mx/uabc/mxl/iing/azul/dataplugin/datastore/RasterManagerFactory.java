package mx.uabc.mxl.iing.azul.dataplugin.datastore;


public class RasterManagerFactory {
    private static PostgisRasterDAO postgresManager;

    public static RasterDataManager getRasterManager(String name) {
        RasterDataManager manager = null;
        if (name.equalsIgnoreCase("postgresql")) {
            if(postgresManager == null) postgresManager = new PostgisRasterDAO();

            manager = postgresManager;
        }

        return manager;
    }
}