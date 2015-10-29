package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.database.ReadPreference;

import java.util.Set;

public class ClientRequestRead extends ClientRequest {
    private Set<String> fields;

    public ReadPreference getReadPreference() {
        return readPreference;
    }

    private ReadPreference readPreference;

    @SuppressWarnings("unused")
    private ClientRequestRead() {
    }

    public ClientRequestRead(String table, String key, Set<String> fields, String destinationNode, ReadPreference readPreference) {
        super(table, key, destinationNode);
        this.fields = fields;
        this.readPreference = readPreference;
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
