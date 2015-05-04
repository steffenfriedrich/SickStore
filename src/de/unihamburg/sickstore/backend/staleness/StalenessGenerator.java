package de.unihamburg.sickstore.backend.staleness;

import java.util.Map;
import java.util.Set;

import de.unihamburg.sickstore.database.messages.ClientRequest;

public interface StalenessGenerator {

    /**
     *
     * @param servers a set with all server ids
     * @param readingServer the reading server's responseId
     * @param request
     * @return a Map of Server to Delay
     */
    Map<Integer, Long> get(Set<Integer> servers, int readingServer, ClientRequest request);
}