package de.unihamburg.sickstore.backend.staleness;

import java.util.Map;

import de.unihamburg.sickstore.database.messages.ClientRequest;

public interface StalenessGenerator {

    public abstract Map<Integer, Long> get(int server, ClientRequest request);

}