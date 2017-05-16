package mx.uabc.mxl.iing.azul.dataplugin.datastore;

import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

//TODO: Use Mongo API instead of calling mongoimport process...
public class CopyMongoDBStrategy implements CopyStrategy {

    @Override
    public boolean copy(String database, String encoding, String fileType, String... filePaths) {
        boolean result = true;
        for(String fileName : filePaths) {
            File file = new File(fileName);
            String fname = file.getName();
            String collection = fname.substring(0, fname.lastIndexOf('.'));
            String[] command = {"mongoimport", "--db", database, "--collection", collection,
                    "--type", fileType, "--file", fileName, "--stopOnError", "--quiet"};

            try {
                MessageMediator.sendMessage("Loading file to MongoDB: " + Arrays.asList(command));
                result = ProcessUtil.executeProcess(command);
            } catch (IOException | InterruptedException e) {
                MessageMediator.sendMessage(e.toString(), MessageMediator.ERROR_MESSAGE);
                result = false;
            }
        }
        return result;
    }

}