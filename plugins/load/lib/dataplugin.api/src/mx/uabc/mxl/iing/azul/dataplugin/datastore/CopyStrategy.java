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

/**
 * CopyStrategy interface to be implemented for any storage system.
 *
 * @author aherrera
 * @version %I%
 */
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