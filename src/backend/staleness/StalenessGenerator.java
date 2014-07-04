package backend.staleness;

import java.util.Map;

import database.messages.ClientRequest;

public interface StalenessGenerator {

    public abstract Map<Integer, Long> get(int server, ClientRequest request);

}