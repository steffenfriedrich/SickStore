package de.unihamburg.sickstore.database;

import com.esotericsoftware.kryonet.Connection;
import de.unihamburg.sickstore.backend.LocalQueryHandler;
import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.Version;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.messages.*;
import de.unihamburg.sickstore.database.messages.exception.DatabaseException;
import java.util.List;
import java.util.Set;

/**
 * Created by Steffen Friedrich on 03.12.2015.
 */
public class LocalSickClient extends Participant {
    String destinationNode;
    QueryHandler queryHandler;
    private TimeHandler timeHandler;

    public LocalSickClient(String destinationNode, String configFile) {
        this.destinationNode = destinationNode;
        this.queryHandler = LocalQueryHandler.getQueryHandler(configFile);
        this.timeHandler = new SystemTimeHandler();
    }
    public LocalSickClient(String destinationNode, String configFile, TimeHandler timeHandler) {
        this.destinationNode = destinationNode;
        this.queryHandler = LocalQueryHandler.getQueryHandler(configFile);
        this.timeHandler = timeHandler;
    }

    public boolean insert(String table, String key, Version values, WriteConcern writeConcern) {
        ClientRequestInsert request = new ClientRequestInsert(table, key, values, writeConcern, destinationNode);
        ServerResponse response = queryHandler.processQuery(request);
        responseSleep(response);
        return true;
    }

    public Version read(String table, String key, Set<String> fields, ReadPreference readPreference) {
        ClientRequestRead request = new ClientRequestRead(table, key, fields, destinationNode, readPreference);
        ServerResponseRead response = (ServerResponseRead) queryHandler.processQuery(request);
        responseSleep(response);
        return response.getVersion();
    }

    public boolean update(String table, String key, Version values, WriteConcern writeConcern) {
        ClientRequestUpdate request = new ClientRequestUpdate(table, key, values, writeConcern, destinationNode);
        ServerResponse response = queryHandler.processQuery(request);
        responseSleep(response);
        return true;
    }

    public boolean delete(String table, String key, WriteConcern writeConcern) throws DatabaseException {
        ClientRequestDelete request = new ClientRequestDelete(table, key, writeConcern, destinationNode);
        ServerResponse response = queryHandler.processQuery(request);
        responseSleep(response);
        return true;
    }

    public List<Version> scan(String table, String startkey, int recordcount,
                              Set<String> fields, boolean ascending, ReadPreference readPreference) throws DatabaseException {
        ClientRequestScan request = new ClientRequestScan(table, startkey, recordcount, fields, ascending, destinationNode, readPreference);
        ServerResponseScan response = (ServerResponseScan) queryHandler.processQuery(request);
        List<Version> entries = response.getEntries();
        responseSleep(response);
        return entries;
    }

    public boolean cleanup(String exportFolder) throws DatabaseException {
        ClientRequestCleanup request = new ClientRequestCleanup(exportFolder);
        ServerResponse response = queryHandler.processQuery(request);
        responseSleep(response);
        return true;
    }

    public void disconnect() {
    }

    private void responseSleep(ServerResponse response) {
        long now = System.currentTimeMillis();
        long latency = now - response.getSentByClientAt();
        long diff = response.getWaitTimeout() - latency;
        if (diff > 0) {
            timeHandler.sleep(diff);
        }
    }
}
