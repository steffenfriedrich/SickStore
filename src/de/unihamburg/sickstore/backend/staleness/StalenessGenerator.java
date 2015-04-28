package de.unihamburg.sickstore.backend.staleness;

import java.util.Map;

import de.unihamburg.sickstore.database.messages.ClientRequest;

public interface StalenessGenerator {

    Map<Integer, Long> get(int server, ClientRequest request);
}