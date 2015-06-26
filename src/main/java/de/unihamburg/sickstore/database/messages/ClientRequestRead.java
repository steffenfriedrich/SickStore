package de.unihamburg.sickstore.database.messages;

import java.util.Set;

public class ClientRequestRead extends ClientRequest {
    private Set<String> fields;

    @SuppressWarnings("unused")
    private ClientRequestRead() {
    }

    public ClientRequestRead(String destinationServer, String table, String key, Set<String> fields) {
        super(destinationServer, table, key);
        this.fields = fields;
    }

    public Set<String> getFields() {
        return fields;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }
}
