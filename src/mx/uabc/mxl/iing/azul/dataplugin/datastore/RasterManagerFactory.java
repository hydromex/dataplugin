package mx.uabc.mxl.iing.azul.dataplugin.datastore;
/*
    Copyright (C) 2018  Jesús Donaldo Osornio Hernández
    Copyright (C) 2018  Luis Alejandro Herrera León
    Copyright (C) 2018  Gabriel Alejandro López Morteo

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
 * Factory for getting a specific {@link RasterDataManager} given a name. Currently only PostgreSQL is supported
 *
 * @author jdosornio
 * @version %I%
 */
public class RasterManagerFactory {
    private static PostgisRasterDAO postgresManager;

    /**
     * Get a given {@link RasterDataManager} specified by name
     *
     * @param name the name of the {@link RasterDataManager}
     *
     * @return an object with the manager requested
     */
    public static RasterDataManager getRasterManager(String name) {
        RasterDataManager manager = null;
        if (name.equalsIgnoreCase("postgresql")) {
            if(postgresManager == null) postgresManager = new PostgisRasterDAO();

            manager = postgresManager;
        }

        return manager;
    }
}