package mx.uabc.mxl.iing.azul.dataplugin.util;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;

import java.util.Map;

public class RestUtil {

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