package de.unihamburg.pimpstore.backend.staleness;

import java.util.Map;

import de.unihamburg.pimpstore.database.messages.ClientRequest;

public interface StalenessGenerator {

    public abstract Map<Integer, Long> get(int server, ClientRequest request);

}