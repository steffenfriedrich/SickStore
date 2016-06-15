package de.unihamburg.sickstore.backend;

import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;

public interface QueryHandlerInterface {
    ServerResponse processQuery(ClientRequest request);
}
