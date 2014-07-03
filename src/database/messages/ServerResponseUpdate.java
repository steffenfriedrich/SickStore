package database.messages;

public class ServerResponseUpdate extends ServerResponse {

	private ServerResponseUpdate() {
		super();
	}
	


	public ServerResponseUpdate(long clientRequestID) {
		super(clientRequestID);
	}
}
