/**
 * 
 */
package de.unihamburg.sickstore.backend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessMap;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.database.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihamburg.sickstore.backend.timer.TimeHandler;

import de.unihamburg.sickstore.database.messages.exception.DeleteException;
import de.unihamburg.sickstore.database.messages.exception.InsertException;
import de.unihamburg.sickstore.database.messages.exception.UpdateException;

/**
 * 
 * This class is responsible for introducing data-centric staleness by serving
 * stale data to the server nodes. </br> To this end, all nodes have to supply
 * the {@link Store} instance with a reference to themselves, so that they get
 * their corresponding degree of staleness.
 * 
 * @author Wolfram Wingerath
 * 
 */
public class Store {

	private static final Logger logStaleness = LoggerFactory
			.getLogger("staleness");

	private TimeHandler timeHandler = new SystemTimeHandler();

	private TreeMap<String, VersionSet> values = new TreeMap<>();

	public Store() {
	}

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

	public void delete(Node node, String key, StalenessMap visibility,
			long timestamp) throws DeleteException {
		Version alreadyExisting = get(node, key, timestamp, false);
		if (alreadyExisting.isNull()) {
			throw new DeleteException(
					"Value cannot be deleted, because there is no value under key \""
							+ key + "\".");
		} else {
			insertOrUpdate(key, new Version(key, node, timeHandler.getCurrentTime(), visibility, true));
		}
	}

	/**
	 * Get a data item with all columns.
	 */
	public synchronized Version get(Node node, String key, long timestamp, boolean logStaleness) {
		return get(node, key, (Set<String>) null, timestamp, logStaleness);
	}

	/**
	 * Get a data item and only read the specific columns (or null for all).
	 *
	 * @param node          the responseId of the reading server
	 * @param key             the requested key
	 * @param columns         set which cointans only specific columns to read (or null to read all)
	 * @param timestamp	      time at which the request is executed
	 * @param logStaleness    to log or not to log staleness?
	 * @return the read version
	 */
	public synchronized Version get(Node node, String key,
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
				if (visibleSince(node, version) <= timestamp) {
					versionStaleness = i;
					break;
				} else {
					version = Version.NULL;
				}
			}
		}

		// log staleness informations for ClientRequestRead only
//		if (logStaleness && version != Version.NULL) {
//			if (version == versionMostRecent) {
//				long timeSinceLastUpdate = timestamp - version.getWrittenAt();
//				Store.logStaleness.info("key;" + key + ";most recent version;"
//						+ versionMostRecent.getWrittenAt()
//						+ ";staleness in versions;" + versionStaleness
//						+ ";staleness in ms;0" + ";read-after-write lag;"
//						+ timeSinceLastUpdate);
//
//			} else {
//				Store.logStaleness.info("key;"
//						+ key
//						+ ";most recent version;"
//						+ versionMostRecent.getWrittenAt()
//						+ ";staleness in versions;"
//						+ versionStaleness
//						+ ";staleness in ms;"
//						+ (timestamp - versionMostRecent.getWrittenAt()
//						+ ";read-after-write lag;" + -1));
//			}
//		}

		if (!version.isNull()) {
			try {
				return version.clone(columns);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return Version.NULL;
	}

	/**
	 * Get a specific data item but only with the value of a single column.
	 */
	public synchronized Version get(Node node, String key, String column,
			long timestamp, boolean logStaleness) {
		if (column == null) {
			throw new IllegalArgumentException("Column must not be null!");
		}

		Set<String> columns = new HashSet<String>();
		columns.add(column);

		return get(node, key, columns, timestamp, logStaleness);
	}

	/**
	 * Read a range of data items.
	 *
	 * @param node
	 * @param key
	 * @param range
	 * @param asc
	 * @param columns
	 * @param timestamp
	 * @return
	 */
	public synchronized List<Version> getRange(Node node, String key,
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
			version = get(node, nextKey, columns, timestamp, true);
			if (!version.isNull()) {
				versions.add(version);
			}
		} while (range > versions.size() && (
			   (asc && (nextKey = higherKey(nextKey)) != null) ||
			   (!asc && (nextKey = lowerKey(nextKey)) != null)
		));

		return versions;
	}

	private synchronized VersionSet getVersionSet(String key) {
		return values.get(key);
	}

	/**
	 * Returns the least key strictly greater than the given key, or null if
	 * there is no such key.
	 *
	 * @param key
	 * @return
	 */
	private synchronized String higherKey(String key) {
		return values.higherKey(key);
	}

	public void insert(Node node, String key, Version version)
			throws InsertException {
		long timestamp = version.getWrittenAt();
		Version alreadyExisting = get(node, key, timestamp, false);
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
	 * @param node
	 *            a server ID
	 * @param version
	 * @return
	 */
	public long visibleSince(Node node, Version version) {
		StalenessMap stalenessWindows = version.getVisibility();
		long writeTimestamp = version.getWrittenAt();
		Long staleness = stalenessWindows.get(node);

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

	public void update(Node node, String key, Version version)
			throws UpdateException {
		Version alreadyExisting = get(node, key, version.getWrittenAt(),
				false);
		if (alreadyExisting.isNull()) {
			throw new UpdateException(
					"Value cannot be updated, because there is no value under key \""
							+ key + "\".");
		} else {
			insertOrUpdate(key, version);
		}
	}
}
