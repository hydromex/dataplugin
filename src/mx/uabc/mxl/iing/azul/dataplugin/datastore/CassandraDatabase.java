package mx.uabc.mxl.iing.azul.dataplugin.datastore;

/**
 * Created by jdosornio on 2/11/16.
 */
public class CassandraDatabase {

//    public static boolean execute(File cqlFile) {
//        if(cqlFile == null || !cqlFile.exists()) {
//            MessageMediator.sendMessage("CQL file is null or empty!: " + cqlFile,
//                    MessageMediator.ERROR_MESSAGE);
//            return false;
//        }
//
//        try {
//            //read file contents
//            String cql = new String(Files.readAllBytes(cqlFile.toPath()));
//            //replace database creation with cluster replication strategy and factor
//            String space = "(:?\\s+|\\n+)";
//            String regex = "CREATE" + space + "(:?KEYSPACE|SCHEMA)" + space +
//                    "(:?IF" + space + "NOT" + space + "EXISTS" + space + ")?" +
//                    "\\.+WITH" + space + "REPLICATION" + space + "=" + space + "(\\{.+\\})";
//            String replicationMap = "{ 'class' : 'SimpleStrategy', 'replication_factor' : 1 }";
//
//            Pattern r = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
//            Matcher m = r.matcher(cql);
//            while(m.find()) {
//                cql = cql.replace(m.group(1), replicationMap);
//                MessageMediator.sendMessage("Modified cql keyspace creation sentence");
//            }
//            //Write modified file
//            Files.write(cqlFile.toPath(), cql.getBytes());
//            //execute cassandra with cqlFile as argument...
//
//
//        } catch (IOException e) {
//            MessageMediator.sendMessage(e.toString(), MessageMediator.ERROR_MESSAGE);
//        }
//
//        return false;
//    }
}