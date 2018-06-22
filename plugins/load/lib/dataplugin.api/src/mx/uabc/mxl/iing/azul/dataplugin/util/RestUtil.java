package mx.uabc.mxl.iing.azul.dataplugin.util;
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

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;

import java.util.Map;

/**
 * Utility class for sending Restful requests
 *
 * @author jdosornio
 * @version %I%
 */
public class RestUtil {

    /**
     * Send a post request to the given url, with the optional headers and field params,
     * and get the response in the specified class type
     *
     * @param url the URL to send the request to
     * @param headers optional map of headers, can be null
     * @param fields optional map of field params, can be null
     * @param type the class type of the response, only Unirest supported ones
     * @param <T> Response class type
     *
     * @return the response body content as the class type specified
     */
    public static <T> T post(String url, Map<String, String> headers,
                             Map<String, Object> fields, Class<T> type) {
        T response = null;
        try {
            response = Unirest.post(url).headers(headers).fields(fields).asObject(type).getBody();
        } catch (UnirestException e) {
            MessageMediator.sendMessage("Error while sending post request! " + e,
                    MessageMediator.ERROR_MESSAGE);
        }

        return response;
    }

    /**
     * Send a get request to the given url, with the optional headers and query params,
     * and get the response in the specified class type
     *
     * @param url the URL to send the request to
     * @param headers optional map of headers, can be null
     * @param params optional map of query params, can be null
     * @param type the class type of the response, only Unirest supported ones
     * @param <T> Response class type
     *
     * @return the response body content as the class type specified
     */
    public static <T> T get(String url, Map<String, String> headers, Map<String, Object> params, Class<T> type) {
        T response = null;

        try {
            response = Unirest.get(url).headers(headers).queryString(params).asObject(type).getBody();
        } catch (UnirestException e) {
            MessageMediator.sendMessage("Error while sending get request! " + e,
                    MessageMediator.ERROR_MESSAGE);
        }

        return response;
    }
}