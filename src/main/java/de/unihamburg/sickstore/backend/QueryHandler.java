package de.unihamburg.sickstore.backend;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import de.unihamburg.sickstore.backend.anomaly.AnomalyGenerator;
import de.unihamburg.sickstore.backend.anomaly.staleness.StalenessMap;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;

import de.unihamburg.sickstore.database.SickServer;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ClientRequestDelete;
import de.unihamburg.sickstore.database.messages.ClientRequestInsert;
import de.unihamburg.sickstore.database.messages.ClientRequestRead;
import de.unihamburg.sickstore.database.messages.ClientRequestScan;
import de.unihamburg.sickstore.database.messages.ClientRequestUpdate;
import de.unihamburg.sickstore.database.messages.ServerResponse;
import de.unihamburg.sickstore.database.messages.ServerResponseDelete;
import de.unihamburg.sickstore.database.messages.ServerResponseException;
import de.unihamburg.sickstore.database.messages.ServerResponseInsert;
import de.unihamburg.sickstore.database.messages.ServerResponseRead;
import de.unihamburg.sickstore.database.messages.ServerResponseScan;
import de.unihamburg.sickstore.database.messages.ServerResponseUpdate;

public class QueryHandler {

	private TimeHandler timeHandler;

	/** Generates server IDs, starting with 1 */
	private final AtomicInteger IDGenerator = new AtomicInteger(1);
	
	/** the overall number of clients connected to the entirety of all Sickstore nodes */
    private final AtomicInteger clientCount = new AtomicInteger(0);
    
    
	private Store mediator;

	protected Set<Node> nodes = new HashSet<>();

	private AnomalyGenerator anomalyGenerator;

	private final MetricRegistry metrics = new MetricRegistry();
    private static final Logger logMetrics = LoggerFactory.getLogger("metrics");
	private Slf4jReporter reporter = null;
	private Meter requests = null;
	private Meter requestsInsert = null;
	private Meter requestsDelete = null;
	private Meter requestsRead= null;
	private Meter requestsScan= null;
	private Meter requestsUpdate= null;

	public QueryHandler(Store mediator,
						AnomalyGenerator anomalyGenerator,
						TimeHandler timeHandler) {
		this.mediator = mediator;
		this.anomalyGenerator = anomalyGenerator;
		this.timeHandler = timeHandler;
	}

	public QueryHandler(Store mediator,
						AnomalyGenerator anomalyGenerator,
						TimeHandler timeHandler,
						Set<Node> nodes) {
		this.mediator = mediator;
		this.anomalyGenerator = anomalyGenerator;
		this.timeHandler = timeHandler;
		this.nodes = nodes;
	}

	public synchronized Set<Node> getNodes() {
		return nodes;
	}

	/**
	 * Handles delete request.
	 */
	private ServerResponseDelete process(ClientRequestDelete request)
			throws NoKeyProvidedException, DeleteException {
		Node node = request.getReceivedBy();
		String key = request.getKey();
		long timestamp = request.getReceivedAt();
		long clientRequestID = request.getId();
		if (key == null) {
			throw new NoKeyProvidedException("Cannot process delete request; no key was provided.");
		}

		StalenessMap visibility = anomalyGenerator.getWriteVisibility(request, getNodes());
		mediator.delete(node, key, visibility, timestamp);

		ServerResponseDelete response = new ServerResponseDelete(clientRequestID);
		anomalyGenerator.handleResponse(response, request, getNodes());

		return response;
	}

	/**
	 * Handles insert request.
	 */
	private ServerResponseInsert process(ClientRequestInsert request)
			throws NoKeyProvidedException, InsertException {
		Node node = request.getReceivedBy();
		String key = request.getKey();
		long timestamp = request.getReceivedAt();
		long clientRequestID = request.getId();
		Version version = request.getVersion();

		if (key == null) {
			throw new NoKeyProvidedException(
					"Cannot process get request; no key was provided.");
		}


		StalenessMap visibility = anomalyGenerator.getWriteVisibility(request, getNodes());
		version.setVisibility(visibility);
		version.setWrittenAt(timestamp);
		version.setWrittenBy(node);
		mediator.insert(node, key, version);

		ServerResponseInsert response = new ServerResponseInsert(clientRequestID);
		anomalyGenerator.handleResponse(response, request, getNodes());

		return response;
	}

	/**
	 * Handles read request.
	 */
	private ServerResponseRead process(ClientRequestRead request)
			throws NoKeyProvidedException {

		Node node = request.getReceivedBy();
		String key = request.getKey();
		Set<String> columns = request.getFields();
		long timestamp = request.getReceivedAt();
		long clientRequestID = request.getId();
		if (key == null) {
			throw new NoKeyProvidedException("Cannot process get request; no key was provided.");
		}

		Version version = mediator.get(node, key, columns, timestamp, true);
		if (version == null) {
			throw new NullPointerException("Version must not be null!");
		}

		ServerResponseRead response = new ServerResponseRead(clientRequestID, version);
		anomalyGenerator.handleResponse(response, request, getNodes());

		return response;
	}

	/**
	 * Handles scan request.
	 */
	private ServerResponseScan process(ClientRequestScan request)
			throws NoKeyProvidedException {
		Node node = request.getReceivedBy();
		String key = request.getKey();
		int range = request.getRecordcount();
		boolean asc = request.isAscending();
		Set<String> columns = request.getFields();
		long timestamp = request.getReceivedAt();
		long clientRequestID = request.getId();
		if (key == null) {
			throw new NoKeyProvidedException(
					"Cannot process get request; no key was provided.");
		}

		List<Version> versions = mediator.getRange(node, key, range, asc, columns, timestamp);
		ServerResponseScan response = new ServerResponseScan(clientRequestID, versions);
		anomalyGenerator.handleResponse(response, request, getNodes());

		return response;
	}

	/**
	 * Handles update request.
	 */
	private ServerResponseUpdate process(ClientRequestUpdate request)
			throws NoKeyProvidedException, UpdateException {
		Node node = request.getReceivedBy();
		String key = request.getKey();
		long timestamp = request.getReceivedAt();
		long clientRequestID = request.getId();
		Version version = request.getVersion();
		if (key == null) {
			throw new NoKeyProvidedException("Cannot process get request; no key was provided.");
		}

		StalenessMap visibility = anomalyGenerator.getWriteVisibility(request, getNodes());
		version.setVisibility(visibility);
		version.setWrittenAt(timestamp);
		mediator.update(node, key, version);

		ServerResponseUpdate response = new ServerResponseUpdate(clientRequestID);
		anomalyGenerator.handleResponse(response, request, getNodes());

		return response;
	}

	/**
	 * Processes an incoming query.
	 */
	public synchronized ServerResponse processQuery(Object request) {
		Long id = -1l;
		ServerResponse response = null;
		try {
			if (request instanceof ClientRequest) {
				ClientRequest clientRequest = (ClientRequest) request;
				clientRequest.setReceivedAt(timeHandler.getCurrentTime());

				id = clientRequest.getId();

				// Find destination node
				if (clientRequest.getDestinationNode() == null) {
					// no destination node given, search primary
					for (Node node : getNodes()) {
						if (node.isPrimary()) {
							clientRequest.setReceivedBy(node);

							break;
						}
					}
				} else {
					for (Node node : getNodes()) {
						if (node.getName().equals(clientRequest.getDestinationNode())) {
							clientRequest.setReceivedBy(node);
						}
					}
				}

				if (clientRequest.getReceivedBy() == null) {
					throw new DatabaseException("Did not find the node this request is pointed to (" + clientRequest.getDestinationNode() + ")");
				}

				// metrics measures "requests per second"
				if (requests == null) {
					initMetricReporter();
					requests = metrics.meter("requests");
				} 
				requests.mark();
			}

			if (request instanceof ClientRequestDelete) {
				// delete request
				response = process((ClientRequestDelete) request);
			} else if (request instanceof ClientRequestInsert) {
				// insert request
				if (requestsInsert == null) {
					requestsInsert = metrics.meter("requestsInsert");
				}
				response = process((ClientRequestInsert) request);
				requestsInsert.mark();
			} else if (request instanceof ClientRequestRead) {
				// read request
				if (requestsRead == null) {
					requestsRead = metrics.meter("requestsRead");
				}
				response = process((ClientRequestRead) request);
				requestsRead.mark();
			} else if (request instanceof ClientRequestScan) {
				// scan request
				if (requestsScan == null) {
					requestsScan = metrics.meter("requestsScan");
				}
				response = process((ClientRequestScan) request);
				requestsScan.mark();
			} else if (request instanceof ClientRequestUpdate) {
				// update request
				if (requestsUpdate == null) {
					requestsUpdate = metrics.meter("requestsUpdate");
				}
				response = process((ClientRequestUpdate) request);
				requestsUpdate.mark();
			} else {
				throw new UnknownMessageTypeException(
						"Cannot process request; unknown message type: "
								+ request.getClass());
			}
		} catch (Exception e) {
			response = new ServerResponseException(id, e);
			e.printStackTrace();
		}
		return response;
	}

	public synchronized void setTimeHandler(TimeHandler timeHandler) {
		this.timeHandler = timeHandler;
	}

	public synchronized void setNodes(Set<Node> nodes) {
		this.nodes = nodes;
	}

	/**
	 * Increment the number of connection clients and return the current number.
	 *
	 * @return number of connected clients
	 */
	public int incrementAndGetClientCount() {
        clientCount.incrementAndGet();
        resetMetersIfIdle();
        return clientCount.get();
    }

	/**
	 * Decrement the number of connection clients and return the current number.
	 *
	 * @return number of connected clients
	 */
    public int decrementAndGetClientCount() {
        clientCount.decrementAndGet();
        resetMetersIfIdle();
        return clientCount.get();
    }

    /**
     * Calls {@link #resetMetres()}, if there are no clients connected to
     * Sickstore
     */
    public void resetMetersIfIdle() {
        if (clientCount.get() == 0) {
            resetMeters();
        }
    }

    public void resetMeters() {
		if (reporter != null) {
			reporter.stop();
			logMetrics.info("reporter stopped");
			for (String name : metrics.getMetrics().keySet()) {
				metrics.remove(name);
			}
			requests = null;
			requestsInsert = null;
		}
    }
    
    public void initMetricReporter() {
		reporter = Slf4jReporter.forRegistry(metrics)
		.outputTo(LoggerFactory.getLogger("metrics"))
		.convertRatesTo(TimeUnit.SECONDS)
		.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		reporter.start(5, TimeUnit.SECONDS);
    	logMetrics.info("reporter started");
    }
}
