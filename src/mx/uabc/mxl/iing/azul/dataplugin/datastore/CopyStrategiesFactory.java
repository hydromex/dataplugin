package mx.uabc.mxl.iing.azul.dataplugin.datastore;


public class CopyStrategiesFactory {

    public static CopyStrategy getStrategy(String strategy){
        CopyStrategy result = null;

        if (strategy.equalsIgnoreCase("mongodb")) {
            result = new CopyMongoDBStrategy();
        } /* else if(strategy.equalsIgnoreCase("postgressql")){
            result = new CopyPostgresStategy();
        } */
        return result;
    }
}