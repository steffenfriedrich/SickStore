package de.unihamburg.sickstore.database.messages;

import java.util.Set;

public class ClientRequestScan extends ClientRequest {
    private boolean ascending = false;
    private Set<String> fields;
    private int recordcount;

    @SuppressWarnings("unused")
    private ClientRequestScan() {
    }

    public ClientRequestScan(String table, String key, int recordcount, Set<String> fields, boolean ascending) {
        super(table, key);
        this.recordcount = recordcount;
        this.fields = fields;
        this.ascending = ascending;
    }

    public ClientRequestScan(String table, String key, int recordcount, Set<String> fields, boolean ascending, String destinationNode) {
        super(table, key, destinationNode);
        this.recordcount = recordcount;
        this.fields = fields;
        this.ascending = ascending;
    }

    public Set<String> getFields() {
        return fields;
    }

    public int getRecordcount() {
        return recordcount;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

    public void setRecordcount(int recordcount) {
        this.recordcount = recordcount;
    }

    @Override
    public String toString() {
        return "SCAN";
    }
}
