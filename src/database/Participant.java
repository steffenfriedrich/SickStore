package database;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import sun.swing.MenuItemLayoutHelper.ColumnAlignment;

import backend.Entry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import database.messages.ClientRequestGet;
import database.messages.ClientRequestGetColumn;
import database.messages.ClientRequestPut;
import database.messages.ClientRequestPutColumn;
import database.messages.ServerResponseException;
import database.messages.ServerResponseGet;
import database.messages.ServerResponseGetColumn;
import database.messages.ServerResponsePut;
import database.messages.exception.DatabaseException;
import database.messages.exception.ExceptionNoColumnProvided;
import database.messages.exception.ExceptionNoKeyProvided;
import database.messages.exception.ExceptionNoValueProvided;
import database.messages.exception.ExceptionUnknownMessageType;

// This class is a convenient place to keep things common to both the client and server.
public class Participant {
    // This registers objects that are going to be sent over the network.
    static public void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();

        for (Class c : new Class[] {
                // Some primitives etc.
                AtomicLong.class,
                ConcurrentHashMap.class,
                Entry.class,
                HashMap.class,
                Long.class,
                Map.class,
                Object.class,
                String.class,
                String[].class,

                // Exceptions
                Exception.class,
                DatabaseException.class,
                ExceptionNoColumnProvided.class,
                ExceptionNoKeyProvided.class,
                ExceptionNoValueProvided.class,
                ExceptionUnknownMessageType.class,
                IllegalArgumentException.class,

                // Messages
                ClientRequestGet.class, ClientRequestGetColumn.class,
                ClientRequestPut.class, ClientRequestPutColumn.class,
                ServerResponseException.class,ServerResponseGetColumn.class, ServerResponseGet.class,
                ServerResponsePut.class }) {
            kryo.register(c);
        }

    }
}
