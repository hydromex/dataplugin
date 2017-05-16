/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.uabc.mxl.iing.azul.dataplugin.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Alex
 */
public class MessageMediator {
    
    //LOGGER
    private static final Logger logger = LogManager.getLogger(MessageMediator.class.getName());
    
    public final static int INFO_MESSAGE = 0;
    public final static int ERROR_MESSAGE = 1;
    public final static int WARNING_MESSAGE = 2;
    
    /**
     * Este metodo envia un mensaje de un plugin para mostrar su retroalimentacion
     * sobre su ejecucion al PluginManager. 
     * @param pluginName Nombre del plugin que envia el mensaje.
     * @param message Mensaje que envia el plugin.
     * @param messageType Tipo de error del mensaje
     */
    public static void sendMessage(String pluginName, String message,
        int messageType){
        if(!pluginName.isEmpty()){
            message = "[PLUGIN] " + pluginName + " [MESSAGE] " + message;
        } else {
            message = "[MESSAGE] " + message;
        }
        switch(messageType){
            case INFO_MESSAGE:
                logger.info(message);
                break;
            case ERROR_MESSAGE:
                logger.error(message);
                break;
            case WARNING_MESSAGE:
                logger.warn(message);
                break;
        }
    }
    
    /**
     * Este metodo envia un mensaje sin especificar la fuente del mensaje.
     * @param message Mensaje a enviar
     * @param messageType Tipo de mensaje
     */
    public static void sendMessage(String message, int messageType){
        sendMessage("", message, messageType);
    }
    
    /**
     * Este metodo envia un mensaje sin especificar la fuente del mensaje y pre-
     * determinadamente sera de tipo informacion.
     * @param message Mensaje a enviar.
     */
    public static void sendMessage(String message){
        sendMessage("", message, INFO_MESSAGE);
    }
}
