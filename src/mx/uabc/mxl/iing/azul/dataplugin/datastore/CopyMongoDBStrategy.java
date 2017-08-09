package mx.uabc.mxl.iing.azul.dataplugin.datastore;
/*
    Copyright (C) 2017  Jesús Donaldo Osornio Hernández
    Copyright (C) 2017  Luis Alejandro Herrera León
    Copyright (C) 2017  Gabriel Alejandro López Morteo

    This file is part of DataPlugin.

    DataPlugin is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataPlugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataPlugin.  If not, see <http://www.gnu.org/licenses/>.
 */

import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;
import mx.uabc.mxl.iing.azul.dataplugin.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * {@link CopyStrategy} implementation for copying data into the applications MongoDB storage system.
 *
 * @author aherrera
 * @author jdosornio
 * @version %I%
 */
public class CopyMongoDBStrategy implements CopyStrategy {

    //TODO: Use Mongo API instead of calling mongoimport process...

    /**
     * Method to copy data to the applications MongoDB storage system. It imports the data from a json file (or files)
     * into the indicated database and encoding.
     *
     * @param database the name of the MongoDB database where to store the imported data
     * @param encoding the encoding that will be used to save the data
     * @param fileType the type of the file(s)
     * @param filePaths the path(s) of the file(s) to import.
     *
     * @return true in case of successfully importing the data to the MongoDB database, false otherwise
     */
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