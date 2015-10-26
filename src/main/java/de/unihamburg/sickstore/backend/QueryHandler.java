package de.unihamburg.sickstore.backend;

import java.util.*;

import de.unihamburg.sickstore.backend.anomaly.Anomaly;
import de.unihamburg.sickstore.backend.anomaly.AnomalyGenerator;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.config.InstanceFactory;
import de.unihamburg.sickstore.database.Node;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.database.messages.exception.*;

public class QueryHandler implements QueryHandlerInterface {

	private TimeHandler timeHandler = new SystemTimeHandler();
	private Store mediator;
	protected Set<Node> nodes = new HashSet<>();
	private AnomalyGenerator anomalyGenerator;

	@SuppressWarnings("unused")
	public static QueryHandlerInterface newInstanceFromConfig(Map<String, Object> config) {
		Map<String, Object> anomalyGeneratorConfig = (Map<String, Object>) config.get("anomalyGenerator");
		if (anomalyGeneratorConfig == null) {
			throw new RuntimeException("Missing anomaly generator");
		}

		Set<Node> nodes = new HashSet<>();
		for (Map<String, Object> node : (List<Map<String, Object>>) config.getOrDefault("nodes", new ArrayList<>())) {
			nodes.add((Node) InstanceFactory.newInstanceFromConfig(node));
		}

		// add nodes into parameters, so that anomaly generators are aware of the available nodes
		anomalyGeneratorConfig.put("nodes", nodes);
		AnomalyGenerator anomalyGenerator = (AnomalyGenerator) InstanceFactory
			.newInstanceFromConfig(anomalyGeneratorConfig);

		return new QueryHandler(new Store(), anomalyGenerator, nodes);
	}

	public QueryHandler(Store mediator,
						AnomalyGenerator anomalyGenerator,
						Set<Node> nodes,
						TimeHandler timeHandler) {
		this(mediator, anomalyGenerator, nodes);
		this.timeHandler = timeHandler;
	}

	public QueryHandler(Store mediator,
						AnomalyGenerator anomalyGenerator,
						Set<Node> nodes) {
		this.mediator = mediator;
		this.anomalyGenerator = anomalyGenerator;
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

		Anomaly anomaly = anomalyGenerator.handleRequest(request, getNodes());
		mediator.delete(node, key, anomaly.getStalenessMap(), timestamp);

		ServerResponseDelete response = new ServerResponseDelete(clientRequestID);
		anomalyGenerator.handleResponse(anomaly, request, response, getNodes());

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


		Anomaly anomaly = anomalyGenerator.handleRequest(request, getNodes());
		version.setVisibility(anomaly.getStalenessMap());
		version.setWrittenAt(timestamp);
		version.setWrittenBy(node);
		version.setKey(request.getKey());
		mediator.insert(node, key, version);

		ServerResponseInsert response = new ServerResponseInsert(clientRequestID);
		anomalyGenerator.handleResponse(anomaly, request, response, getNodes());

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

		Anomaly anomaly = anomalyGenerator.handleRequest(request, getNodes());

		Version version = mediator.get(node, key, columns, timestamp, true);
		if (version == null) {
			throw new NullPointerException("Version must not be null!");
		}

		ServerResponseRead response = new ServerResponseRead(clientRequestID, version);
		anomalyGenerator.handleResponse(anomaly, request, response, getNodes());

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

		Anomaly anomaly = anomalyGenerator.handleRequest(request, getNodes());
		List<Version> versions = mediator.getRange(node, key, range, asc, columns, timestamp);
		ServerResponseScan response = new ServerResponseScan(clientRequestID, versions);
		anomalyGenerator.handleResponse(anomaly, request, response, getNodes());

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

		Anomaly anomaly = anomalyGenerator.handleRequest(request, getNodes());
		version.setVisibility(anomaly.getStalenessMap());
		version.setWrittenAt(timestamp);
		version.setKey(request.getKey());
		mediator.update(node, key, version);

		ServerResponseUpdate response = new ServerResponseUpdate(clientRequestID);
		anomalyGenerator.handleResponse(anomaly, request, response, getNodes());

		return response;
	}

	private ServerResponseCleanup process(ClientRequestCleanup request) {
		long clientRequestID = request.getId();
		Anomaly anomaly = anomalyGenerator.handleRequest(request, getNodes());
		// TODO export measurement
		ServerResponseCleanup response = new ServerResponseCleanup(clientRequestID);
		anomalyGenerator.handleResponse(anomaly, request, response, getNodes());
 		return response;
	}

	/**
	 * Processes an incoming query.
	 */
	@Override
	public synchronized ServerResponse processQuery(ClientRequest request) {
		Long id = -1l;
		ServerResponse response = null;
		try {
			request.setReceivedAt(timeHandler.getCurrentTime());

			id = request.getId();

			// Find destination node
			if (request.getDestinationNode() == null) {
				// no destination node given, search primary
				for (Node node : getNodes()) {
					if (node.isPrimary()) {
						request.setReceivedBy(node);

						break;
					}
				}
			} else {
				for (Node node : getNodes()) {
					if (node.getName().equals(request.getDestinationNode())) {
						request.setReceivedBy(node);
					}
				}
			}
			if (request.getReceivedBy() == null) {
				throw new DatabaseException("Did not find the node this request is pointed to (" + request.getDestinationNode() + ")");
			}
			if (request instanceof ClientRequestDelete) {
				// delete request
				response = process((ClientRequestDelete) request);
			} else if (request instanceof ClientRequestInsert) {
				// insert request
				response = process((ClientRequestInsert) request);
			} else if (request instanceof ClientRequestRead) {
				// read request
				response = process((ClientRequestRead) request);
			} else if (request instanceof ClientRequestScan) {
				// scan request
				response = process((ClientRequestScan) request);
			} else if (request instanceof ClientRequestUpdate) {
				// update request
				response = process((ClientRequestUpdate) request);
			} else if (request instanceof ClientRequestCleanup) {
				// cleanup request
				response = process((ClientRequestCleanup) request);
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
}
