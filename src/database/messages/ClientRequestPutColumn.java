package database.messages;

public class ClientRequestPutColumn extends ClientRequest {
    private Object value;
    private String column;

    private ClientRequestPutColumn() {
    }

    public ClientRequestPutColumn(String key, String column, Object value) {
        super();
        this.key = key;
        this.column = column;
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
