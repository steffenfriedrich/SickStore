package de.unihamburg.sickstore.database.messages;

import java.util.Set;

public class ClientRequestRead extends ClientRequest {
    private Set<String> fields;

    @SuppressWarnings("unused")
    private ClientRequestRead() {
    }

    public ClientRequestRead(String table, String key, Set<String> fields, String destinationNode) {
        super(table, key, destinationNode);
        this.fields = fields;
    }

    public ClientRequestRead(String table, String key, Set<String> fields) {
        super(table, key);
        this.fields = fields;
    }

    public Set<String> getFields() {
        return fields;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "READ";
    }
}
