package de.unihamburg.sickstore.database.client;

import java.sql.SQLException;

public interface Client {
    Connection getConnection() throws SQLException;

    String getHost();

    int getPort();
}
