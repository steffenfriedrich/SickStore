package database;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import backend.Version;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import com.google.common.reflect.ClassPath;

import database.messages.ClientRequest;
import database.messages.exception.DatabaseException;

// This class is a convenient place to keep things common to both the client and server.
public class Participant {
	// This registers objects that are going to be sent over the network.
	static public void register(EndPoint endPoint) throws IOException {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		// register some primitives etc.
		classes.add(AtomicLong.class);
		classes.add(ConcurrentHashMap.class);
		classes.add(Version.class);
		classes.add(Exception.class);
		classes.add(HashMap.class);
		classes.add(IllegalArgumentException.class);
		classes.add(Long.class);
		classes.add(Map.class);
		classes.add(Object.class);
		classes.add(String.class);
		classes.add(String[].class);

		// register messages
		classes.addAll(getClassesInPackageOf(ClientRequest.class));
		// register exceptions
		classes.addAll(getClassesInPackageOf(DatabaseException.class));

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
}
