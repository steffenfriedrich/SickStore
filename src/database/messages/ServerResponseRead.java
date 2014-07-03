package database.messages;

import backend.Version;

public class ServerResponseRead extends ServerResponse {
	private Version entry;

	private ServerResponseRead() {
		super(); 
	}

	public ServerResponseRead(long clientRequestID, Version entry) { 
	super(clientRequestID);
		this.entry = entry;
	}

	public Version getEntry() {
		return entry;
	}

	public void setEntry(Version entry) {
		this.entry = entry;
	}
}
