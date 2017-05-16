package mx.uabc.mxl.iing.azul.dataplugin.datastore;


public interface CopyStrategy {
    /**
     * Copies the files to the database, this method assumes the
     * name of the files are the names of the tables/collections
     * @param database Database name
     * @param encoding The encoding of the files to read, if null or empty the default is UTF-8
     * @param fileType Type of files to copy e.g. CSV or JSON
     * @param filePaths Array of files paths
     * @return a boolean that's false if the copy failed or true
     * if it was successful
     */
    public boolean copy(String database, String encoding, String fileType, String... filePaths);
}