package mx.uabc.mxl.iing.azul.dataplugin.logger;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Class used for logging application messages of different levels of importance.
 * Used both by the main application and a plugin application.
 *
 * @author aherrera
 * @version %I%
 */
public class MessageMediator {
    
    //LOGGER
    private static final Logger logger = LogManager.getLogger(MessageMediator.class.getName());
    
    public final static int INFO_MESSAGE = 0;
    public final static int ERROR_MESSAGE = 1;
    public final static int WARNING_MESSAGE = 2;
    
    /**
     * This method sends a plugin message to show the feedback about its execution
     *
     * @param pluginName Plugin name that sends the message
     * @param message Message sent
     * @param messageType Message error type
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
     * This method sends a message without specifying the message source
     *
     * @param message Message to send
     * @param messageType Message type
     */
    public static void sendMessage(String message, int messageType){
        sendMessage("", message, messageType);
    }
    
    /**
     * This method sends an information type message without specifying its source
     *
     * @param message Message to send
     */
    public static void sendMessage(String message){
        sendMessage("", message, INFO_MESSAGE);
    }
}
