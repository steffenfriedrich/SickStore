package database.messages;

import backend.Version;

public class ClientRequestUpdate extends ClientRequest {
	private Version version;

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	private ClientRequestUpdate() {
	}

	public ClientRequestUpdate(String table, String key, Version version) {
		super(table, key);
		this. version = version;
	}
}
