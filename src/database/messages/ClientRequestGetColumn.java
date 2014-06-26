package database.messages;

public class ClientRequestGetColumn extends ClientRequest {

    private String column;

    private ClientRequestGetColumn() {
    }

    public ClientRequestGetColumn(String key, String column) {
        super();
        this.key = key;
        this.column = column;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }
}
