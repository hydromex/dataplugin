package mx.uabc.mxl.iing.azul.dataplugin.descriptor;
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

import java.util.Map;

/**
 * Class representing an instance of the applications configuration file
 *
 * @author jdosornio
 * @version %I%
 */
public class Configuration {
    private final Map CONFIG_ROOT;
    private static final String CONFIG_KEY = "config";
    private static final String PLUGIN_DIR = "plugin-dir";
    private static final String CATALOG_DATABASE = "catalog-database";
    private static final String MONGO_USERNAME = "mongodb-username";
    private static final String MONGO_PASSWORD = "mongodb-password";
    private static final String MONGO_HOST = "mongodb-host";
    private static final String MONGO_PORT = "mongodb-port";
    private static final String MONGO_AUTH_DB = "mongodb-authdb";
    private static final String PG_USERNAME = "pg-username";
    private static final String PG_PASS = "pg-password";
    private static final String PG_HOST = "pg-host";
    private static final String PG_PORT = "pg-port";
    private static final String PG_GRID_DB = "pg-raster-db";

    /**
     * Creates a new Configuration object from the given Map object
     *
     * @param config the map object
     */
    Configuration(Map config) {
        CONFIG_ROOT = (Map) config.get(CONFIG_KEY);
    }

    /**
     *
     * @return the application plugins directory path
     */
    public String getPluginDir() {
        return CONFIG_ROOT.get(PLUGIN_DIR).toString();
    }

    /**
     *
     * @return the name of the MongoDB catalog database
     */
    public String getCatalogDatabase() {
        return CONFIG_ROOT.get(CATALOG_DATABASE).toString();
    }

    /**
     *
     * @return the MongoDB username used to connect to the catalog database
     */
    public String getMongoUsername() {
        return CONFIG_ROOT.get(MONGO_USERNAME).toString();
    }

    /**
     *
     * @return the MongoDB password used to connect to the catalog database
     */
    public String getMongoPassword() {
        return CONFIG_ROOT.get(MONGO_PASSWORD).toString();
    }

    /**
     *
     * @return the host where the MongoDB instance with the catalog database is running
     */
    public String getMongoHost() {
        return CONFIG_ROOT.get(MONGO_HOST).toString();
    }

    /**
     *
     * @return the port used to connect to the MongoDB host
     */
    public int getMongoPort() {
        return Integer.parseInt(CONFIG_ROOT.get(MONGO_PORT).toString());
    }

    /**
     *
     * @return the MongoDB database used to authenticate the catalog database user
     */
    public String getMongoAuthDb() {
        return CONFIG_ROOT.get(MONGO_AUTH_DB).toString();
    }

    /**
     *
     * @return the PostgreSQL username used to connect to the application grid database
     */
    public String getPostgresUser() {
        return CONFIG_ROOT.get(PG_USERNAME).toString();
    }

    /**
     *
     * @return the PostgreSQL password used to connect to the grid database
     */
    public String getPostgresPass() {
        return CONFIG_ROOT.get(PG_PASS).toString();
    }

    /**
     *
     * @return the host where the PostgreSQL instance with the grid database is running
     */
    public String getPostgresHost() {
        return CONFIG_ROOT.get(PG_HOST).toString();
    }

    /**
     *
     * @return the port used to connect to the PostgreSQL host
     */
    public int getPostgresPort() {
        return Integer.parseInt(CONFIG_ROOT.get(PG_PORT).toString());
    }

    /**
     *
     * @return the name of the PostgreSQL grid database
     */
    public String getPostgresGridDB() {
        return CONFIG_ROOT.get(PG_GRID_DB).toString();
    }

}