/**
 * 
 */
package de.unihamburg.sickstore.backend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import de.unihamburg.sickstore.backend.timer.TimeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihamburg.sickstore.database.messages.exception.DeleteException;
import de.unihamburg.sickstore.database.messages.exception.InsertException;
import de.unihamburg.sickstore.database.messages.exception.UpdateException;

/**
 * 
 * This class is responsible for introducing data-centric staleness by serving
 * stale data to the server nodes. </br> To this end, all servers have to supply
 * the {@link Store} instance with a reference to themselves, so that they get
 * their corresponding degree of staleness.
 * 
 * @author Wolfram Wingerath
 * 
 */
public class Store {

	private static final Logger logMeasure = LoggerFactory
			.getLogger("measurements");

	private TimeHandler timeHandler;

	private TreeMap<String, VersionSet> values = new TreeMap<String, VersionSet>();

	public Store(TimeHandler timeHandler) {
		this.timeHandler = timeHandler;
	}

	/**
	 * Clears the Datastore from all data
	 * 
	 */
	public void clear() {
		values.clear();
		System.gc();
	}

	public void delete(int server, String key, Map<Integer, Long> visibility,
			long timestamp) throws DeleteException {
		Version alreadyExisting = get(server, key, timestamp, false);
		if (alreadyExisting.isNull()) {
			throw new DeleteException(
					"Value cannot be deleted, because there is no value under key \""
							+ key + "\".");
		} else {
			insertOrUpdate(key, new Version(server, timeHandler.getCurrentTime(), visibility, true));
		}
	}

	public synchronized Version get(int server, String key, long timestamp,
			boolean logStaleness) {
		return get(server, key, (Set<String>) null, timestamp, logStaleness);
	}

	/**
	 * Get a data item.
	 *
	 * @param server          the id of the reading server
	 * @param key             the requested key
	 * @param columns         set which cointans only specific columns to read
	 * @param timestamp	      time at which the request is executed
	 * @param logStaleness    to log or not to log staleness?
	 * @return
	 */
	public synchronized Version get(int server, String key,
			Set<String> columns, long timestamp, boolean logStaleness) {
		if (key == null) {
			throw new NullPointerException("Key must not be null!");
		}

		VersionSet versions = getVersionSet(key);
		Version version = Version.NULL;
		Version versionMostRecent = Version.NULL;
		int versionStaleness = 0;
		// find the version that was up-to-date (most recent) at the given
		// timestamp
		if (versions != null) {
			versionMostRecent = versions.get(0);
			for (int i = 0; i < versions.size(); i++) {
				version = versions.get(i);
				if (visibleSince(server, version) <= timestamp) {
					versionStaleness = i;
					break;
				} else {
					version = Version.NULL;
				}
			}
		}

		// log staleness informations for ClientRequestRead only
		if (logStaleness) {
			if (version == versionMostRecent) {
				long timeSinceLastUpdate = timestamp - version.getWrittenAt();
				logMeasure.info("key;" + key + ";most recent version;"
						+ versionMostRecent.getWrittenAt()
						+ ";staleness in versions;" + versionStaleness
						+ ";staleness in ms;0" + ";read-after-write lag;"
						+ timeSinceLastUpdate);

			} else {
				logMeasure.info("key;"
						+ key
						+ ";most recent version;"
						+ versionMostRecent.getWrittenAt()
						+ ";staleness in versions;"
						+ versionStaleness
						+ ";staleness in ms;"
						+ (timestamp - versionMostRecent.getWrittenAt()
								+ ";read-after-write lag;" + -1));
			}
		}

		if (!version.isNull()) {
			try {
				return version.clone(columns);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return Version.NULL;
	}

	public synchronized Version get(int server, String key, String column,
			long timestamp, boolean logStaleness) {
		if (column == null) {
			throw new IllegalArgumentException("Column must not be null!");
		}
		Set<String> columns = new HashSet<String>();
		columns.add(column);
		return get(server, key, columns, timestamp, logStaleness);
	}

	public synchronized List<Version> getRange(int server, String key,
			int range, boolean asc, Set<String> columns, long timestamp) {
		if (key == null) {
			throw new IllegalArgumentException("Key must not be null!");
		}
		if (range < 1) {
			throw new IllegalArgumentException(
					"Range must be greater than or equal to 1!");
		}

		List<Version> versions = new ArrayList<Version>(range);
		String nextKey = key;
		Version version = null;

		do {
			version = get(server, nextKey, columns, timestamp, true);
			if (!version.isNull()) {
				versions.add(version);
			}
		} while (range > versions.size()
				&& (asc && (nextKey = higherKey(nextKey)) != null || !asc
						&& (nextKey = lowerKey(nextKey)) != null));

		return versions;
	}

	private synchronized VersionSet getVersionSet(String key) {
		return values.get(key);
	}

	/**
	 * Returns the least key strictly greater than the given key, or null if
	 * there is no such key.
	 * 
	 * 
	 * @param key
	 * @return
	 */
	private synchronized String higherKey(String key) {
		return values.higherKey(key);
	}

	public void insert(int server, String key, Version version)
			throws InsertException {
		long timestamp = version.getWrittenAt();
		Version alreadyExisting = get(server, key, timestamp, false);
		if (alreadyExisting.isNull()) {
			insertOrUpdate(key, version);
		} else {
			throw new InsertException(
					"Value cannot be stored, because there already is a value under key \""
							+ key + "\".");
		}
	}

	/**
	 * Stores the given entry value under the given key and timestamp.
	 * 
	 * @param key
	 * @param value
	 */
	public synchronized void insertOrUpdate(String key, Version value) {
		VersionSet entrySet = values.get(key);

		if (entrySet == null) {
			entrySet = new VersionSet();

			values.put(key, entrySet);
		}
		entrySet.add(0, value);
	}

	/**
	 * Calculates the timestamp when a server can see a specific version.
	 * 
	 * @param server
	 *            a server ID
	 * @param version
	 * @return
	 */
	public long visibleSince(int server, Version version) {
		Map<Integer, Long> stalenessWindows = version.getVisibility();
		long writeTimestamp = version.getWrittenAt();
		Long staleness = stalenessWindows.get(server);

		if (staleness == null) {
			return -1l;
		} else {
			long visibleSince = writeTimestamp + staleness;

			return visibleSince;
		}
	}

	/**
	 * Returns the greatest key strictly less than the given key, or null if
	 * there is no such key.
	 * 
	 * 
	 * @param key
	 * @return
	 */
	private synchronized String lowerKey(String key) {
		return values.lowerKey(key);
	}

	@Override
	public String toString() {
		return values.toString();
	}

	public void update(int server, String key, Version version)
			throws UpdateException {
		Version alreadyExisting = get(server, key, version.getWrittenAt(),
				false);
		if (alreadyExisting.isNull()) {
			throw new UpdateException(
					"Value cannot be updated, because there is no value under key \""
							+ key + "\".");
		} else {
			insertOrUpdate(key, version);
		}
	}

	public void setTimeHandler(TimeHandler timeHandler) {
		this.timeHandler = timeHandler;
	}

	public TimeHandler getTimeHandler() {
		return timeHandler;
	}
}
