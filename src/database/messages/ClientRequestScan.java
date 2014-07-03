package database.messages;

import java.util.Set;

public class ClientRequestScan extends ClientRequest {
	private int recordcount;
	private Set<String> fields;
	private boolean ascending= false;

	public int getRecordcount() {
		return recordcount;
	}

	public void setRecordcount(int recordcount) {
		this.recordcount = recordcount;
	} 

	public boolean isAscending() {
		return ascending;
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	public Set<String> getFields() {
		return fields;
	}

	public void setFields(Set<String> fields) {
		this.fields = fields;
	}

	private ClientRequestScan() { 
	}

	public ClientRequestScan(String table, String key, int recordcount,
			Set<String> fields,boolean asc) {
		super(table, key);
		this.recordcount = recordcount;
		this.fields = fields;
		this.ascending = ascending;
	}
}
