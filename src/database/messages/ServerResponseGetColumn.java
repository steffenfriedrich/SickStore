package database.messages;

import backend.Entry;

public class ServerResponseGetColumn extends ServerResponse {
    private  Object value;

    private ServerResponseGetColumn() {
    }

    public ServerResponseGetColumn(Long id, Object value) {
        super();
        this.id = id;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
