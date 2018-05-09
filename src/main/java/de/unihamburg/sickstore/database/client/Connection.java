package de.unihamburg.sickstore.database.client;

import de.unihamburg.sickstore.database.messages.ClientRequest;

import java.sql.SQLException;

public interface Connection extends AutoCloseable {

    void close() throws SQLException;

    public boolean isClosed();

    SickConnection.ResponseHandler write(SickConnection.ResponseCallback callback);
}
