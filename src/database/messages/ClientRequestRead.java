package database.messages;

import java.util.Set;

public class ClientRequestRead extends ClientRequest {
	private Set<String> fields;

	public Set<String> getFields() {
		return fields;
	}

	public void setFields(Set<String> fields) {
		this.fields = fields;
	}

	private ClientRequestRead() { 
	}

	public ClientRequestRead(String table, String key, Set<String> fields) {
		super(table, key);
		this.fields = fields;
	}
}
