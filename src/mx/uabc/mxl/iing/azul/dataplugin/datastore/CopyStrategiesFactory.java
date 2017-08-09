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
 * @author aherrera
 * @author jdosornio
 * @version %I%
 */
public class CopyStrategiesFactory {

    /**
     * Get the CopyStrategy for a given copy strategy key, It is the standard way to copy data into the underlying
     * application storage systems.
     *
     * @param strategy copy strategy key of the strategy to get
     * @return a {@link CopyStrategy} instance if found, else null
     */
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