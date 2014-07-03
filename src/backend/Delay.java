/**
 * 
 */
package backend;

import java.util.HashMap;
import java.util.Map;

import database.messages.ClientRequest;

/**
 * @author Wolfram Wingerath
 * 
 */
public class Delay {
	
	private QueryHandler handler=QueryHandler.getInstance();

	public Map<Integer, Long> get(int server, ClientRequest request) {
		HashMap<Integer, Long> delay =new HashMap<Integer, Long>();
		for (Integer s : handler.getServers()) {
			delay.put(s, 0l);
		}
		// TODO dummy implementation

		return delay;
	}
}
