package database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.reflections.Reflections;

import backend.Entry;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import database.messages.ClientRequest;
import database.messages.exception.DatabaseException;

// This class is a convenient place to keep things common to both the client and server.
public class Participant {
	// This registers objects that are going to be sent over the network.
	static public void register(EndPoint endPoint) {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		// register some primitives etc.
		classes.add(AtomicLong.class);
		classes.add(ConcurrentHashMap.class);
		classes.add(Entry.class);
		classes.add(Exception.class);
		classes.add(HashMap.class);
		classes.add(IllegalArgumentException.class);
		classes.add(Long.class);
		classes.add(Map.class);
		classes.add(Object.class);
		classes.add(String.class);
		classes.add(String[].class);

		// register exceptions
		classes.addAll(getClassesInPackageOf(DatabaseException.class));
		// register messages
		classes.addAll(getClassesInPackageOf(ClientRequest.class));

		Kryo kryo = endPoint.getKryo();
		for (Class<?> c : classes) {
			kryo.register(c);
		}
	}

	/**
	 * 
	 * @param c
	 *            a class
	 * @return all classes that are part of the same package as <code>c</code>
	 */
	private static Set<Class<? extends Object>> getClassesInPackageOf(Class<?> c) {
		Reflections reflections = new Reflections(c.getName());

		Set<Class<? extends Object>> allClasses = reflections
				.getSubTypesOf(Object.class);

		return allClasses;
	}
}
