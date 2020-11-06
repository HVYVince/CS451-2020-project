package cs451;

import java.util.ArrayList;
import java.util.HashMap;

public class ReliableBroadcast implements Registerable {
	ArrayList<String> delivered = new ArrayList<>();
	Parser parser;
	HashMap<Integer, PerfectLink> links;
	
	public ReliableBroadcast(HashMap<Integer, PerfectLink> links, Parser parser) {
		this.links = links;
		this.parser = parser;
	}
	
	public void broadcast(String m) {
    	for(int id : links.keySet()) {
    		if(id == parser.myId())
    			continue;
    		PerfectLink link = links.get(id);
    		link.send(m);
    	}
    	deliver(parser.myId(), m);
    	delivered.add(m);
    }
	
	public void deliver(int from, String m) {
		Logger.log.add("d " + Integer.toString(from) + " " + m);
	}

	@Override
	public void handleMessage(int from, String m) {
		if(delivered.contains(m))
			return;
		
		for(Host host : parser.hosts()) {
			if(host.getId() == from || host.getId() == parser.myId()) {
				PerfectLink link = links.get(host.getId());
				link.send(m);
			}
		}
		deliver(from, m);
		delivered.add(m);
	}
}
