/**
 * 
 */
package backend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.jsonbeans.ObjectMap.Keys;
import com.esotericsoftware.kryonet.Connection;

import database.PIMPServer;
import database.messages.ClientRequest;
import database.messages.ClientRequestDelete;
import database.messages.ClientRequestInsert;
import database.messages.ClientRequestRead;
import database.messages.ClientRequestScan;
import database.messages.ClientRequestUpdate;
import database.messages.ServerResponse;
import database.messages.ServerResponseDelete;
import database.messages.ServerResponseException;
import database.messages.ServerResponseInsert;
import database.messages.ServerResponseRead;
import database.messages.ServerResponseUpdate;
import database.messages.exception.DatabaseException;
import database.messages.exception.ExceptionNoKeyProvided;
import database.messages.exception.ExceptionUnknownMessageType;

/**
 * 
 * This class is responsible for introducing data-centric staleness by serving
 * stale data to the server nodes. </br> To this end, all servers have to supply
 * the {@link Mediator} instance with a reference to themselves, so that they
 * get their corresponding degree of staleness.
 * 
 * @author Wolfram Wingerath
 * 
 */
public class Mediator {
	private final static Mediator instance;

	private Store store = Store.getInstance();
	static {
		instance = new Mediator();
	}

	private Mediator() {
	}

	public static Mediator getInstance() {
		return instance;
	}

	public Version get(int server, String key, long timestamp) {
		return get(server, key, (Set<String>) null, timestamp);
	}

	public Version get(int server, String key, Set<String> columns,
			long timestamp) {
		if (columns == null) {
			throw new NullPointerException("Columns must not be null!");
		}
		if (key == null) {
			throw new NullPointerException("Key must not be null!");
		}

		VersionSet versions = store.get(key);
		Version version = null;

		// find the version that was up-to-date (most recent) at the given
		// timestamp
		for (long t : versions.descendingKeySet()) {
			version = versions.get(t);
			if (version.isVisible(server, timestamp)) {
				break;
			}
		}

		try {
			return version.clone(columns);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Version get(int server, String key, String column, long timestamp) {
		if (column == null) {
			throw new IllegalArgumentException("Column must not be null!");
		}
		Set<String> columns = new HashSet<String>();
		columns.add(column);
		return get(server, key, columns, timestamp);
	}

	public List<Version> getRange(int server, String key, int range,
			boolean asc, Set<String> columns, long timestamp) {
		List<String> keys = store.getKeyRange(key, range, asc);
		List<Version> versions = new ArrayList<Version>();

		for (String k : keys) {
			versions.add(get(server, k, columns, timestamp));
		}
		return versions;
	}

	public void put(String key, Version version, long timestamp) {
		store.put(key, version, timestamp);
	}
}
