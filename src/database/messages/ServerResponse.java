package database.messages;

public class ServerResponse {

    protected Long id;

    public ServerResponse() {
        super();
    }

    public ServerResponse(Long id) {
        this();
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    } 
}