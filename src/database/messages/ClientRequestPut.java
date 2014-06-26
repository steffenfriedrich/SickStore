package database.messages;

import backend.Entry;

public class ClientRequestPut extends ClientRequest {
    private Entry value;

    private ClientRequestPut() {
    }

    public ClientRequestPut(String key, Entry value) {
        super();
        this.key = key;
        this.value = value;
    }

    public Entry getValue() {
        return value;
    }

    public void setValue(Entry value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
