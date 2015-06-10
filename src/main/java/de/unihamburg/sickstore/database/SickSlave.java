package de.unihamburg.sickstore.database;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.database.messages.exception.WriteForbiddenException;

/**
 * A sick slave is a replication server which acts as a slave.
 * It is not allowed to write to this server but reading is possible.
 */
public class SickSlave extends SickServer {

    public SickSlave(int port, QueryHandler queryHandler, TimeHandler timeHandler) {
        super(port, queryHandler, timeHandler);
    }

    @Override
    protected ServerResponse handleRequest(ClientRequest request) {
        if (request instanceof ClientRequestInsert ||
                request instanceof ClientRequestDelete ||
                request instanceof ClientRequestUpdate) {
            return new ServerResponseException(
                -1,
                new WriteForbiddenException("It is not allowed to write to a slave")
            );
        }

        return super.handleRequest(request);
    }
}
