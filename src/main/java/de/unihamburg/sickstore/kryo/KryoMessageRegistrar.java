package de.unihamburg.sickstore.kryo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.esotericsoftware.kryo.Kryo;
import com.google.common.reflect.ClassPath;

import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.database.ReadPreference;
import de.unihamburg.sickstore.database.WriteConcern;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.database.messages.exception.DatabaseException;
import de.unihamburg.sickstore.database.messages.exception.DeleteException;
import de.unihamburg.sickstore.database.messages.exception.DoubleVersionException;
import de.unihamburg.sickstore.database.messages.exception.InsertException;
import de.unihamburg.sickstore.database.messages.exception.NoColumnProvidedException;
import de.unihamburg.sickstore.database.messages.exception.NoKeyProvidedException;
import de.unihamburg.sickstore.database.messages.exception.NoValueProvidedException;
import de.unihamburg.sickstore.database.messages.exception.NotConnectedException;
import de.unihamburg.sickstore.database.messages.exception.UnknownMessageTypeException;
import de.unihamburg.sickstore.database.messages.exception.UpdateException;

// This class is a convenient place to keep things common to both the client and server.
public class KryoMessageRegistrar {
    /**
     * 
     * @param c
     *            a class
     * @return all classes that are part of the same package as <code>c</code>
     * @throws IOException
     */
    private static Set<Class<? extends Object>> getClassesInPackageOf(Class<?> c)
            throws IOException {
        String pack = c.getPackage().getName();
        final ClassLoader loader = Thread.currentThread()
                .getContextClassLoader();

        Set<Class<? extends Object>> allClasses = new HashSet<Class<? extends Object>>();

        for (final ClassPath.ClassInfo info : ClassPath.from(loader)
                .getTopLevelClasses()) {
            if (info.getName().startsWith(pack)) {
                final Class<?> clazz = info.load();
                allClasses.add(clazz);
            }
        }
        return allClasses;
    }

    public static void main(String[] args) throws Exception {
        printClasses();
    }

    private static void printClasses() throws IOException {
        List<String> classes = new ArrayList<String>();

        System.out.println();
        System.out.println("// register messages");
        for (Class<? extends Object> c : getClassesInPackageOf(ClientRequest.class)) {
            classes.add("classes.add(" + c.getSimpleName() + ".class);");
        }
        Collections.sort(classes);
        for (String string : classes) {
            System.out.println(string);
        }
        classes.clear();

        System.out.println();
        System.out.println("// register exceptions");
        for (Class<? extends Object> c : getClassesInPackageOf(DatabaseException.class)) {
            classes.add("classes.add(" + c.getSimpleName() + ".class);");
        }
        Collections.sort(classes);
        for (String string : classes) {
            System.out.println(string);
        }
        classes.clear();
    }

    // This registers objects that are going to be sent over the network.
    static public void register(Kryo kryo) {
        List<Class<?>> classes = new ArrayList<Class<?>>();

        // register some primitives etc.
        classes.add(ArrayList.class);
        classes.add(AtomicLong.class);
        classes.add(byte[].class);
        classes.add(ConcurrentHashMap.class);
        classes.add(Version.class);
        classes.add(Exception.class);
        classes.add(HashMap.class);
        classes.add(HashSet.class);
        classes.add(IllegalArgumentException.class);
        classes.add(Long.class);
        classes.add(Map.class);
        classes.add(NullPointerException.class);
        classes.add(Object.class);
        classes.add(String.class);
        classes.add(String[].class);
        classes.add(TreeMap.class);

        // register messages
        classes.add(WriteConcern.class);
        classes.add(ReadPreference.class);
        classes.add(ClientRequest.class);
        classes.add(ClientRequestDelete.class);
        classes.add(ClientRequestInsert.class);
        classes.add(ClientRequestRead.class);
        classes.add(ClientRequestScan.class);
        classes.add(ClientRequestUpdate.class);
        classes.add(ClientRequestCleanup.class);
        classes.add(DatabaseException.class);
        classes.add(DeleteException.class);
        classes.add(DoubleVersionException.class);
        classes.add(InsertException.class);
        classes.add(NoColumnProvidedException.class);
        classes.add(NoKeyProvidedException.class);
        classes.add(NoValueProvidedException.class);
        classes.add(NotConnectedException.class);
        classes.add(ServerResponse.class);
        classes.add(ServerResponseDelete.class);
        classes.add(ServerResponseException.class);
        classes.add(ServerResponseInsert.class);
        classes.add(ServerResponseRead.class);
        classes.add(ServerResponseScan.class);
        classes.add(ServerResponseUpdate.class);
        classes.add(ServerResponseCleanup.class);
        classes.add(UnknownMessageTypeException.class);
        classes.add(UpdateException.class);

        // register exceptions
        classes.add(DatabaseException.class);
        classes.add(DeleteException.class);
        classes.add(DoubleVersionException.class);
        classes.add(InsertException.class);
        classes.add(NoColumnProvidedException.class);
        classes.add(NoKeyProvidedException.class);
        classes.add(NoValueProvidedException.class);
        classes.add(NotConnectedException.class);
        classes.add(UnknownMessageTypeException.class);
        classes.add(UpdateException.class);

        for (Class<?> c : classes) {
            kryo.register(c);
        }
    }
}
