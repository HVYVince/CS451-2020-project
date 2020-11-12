package cs451;

import java.util.ArrayList;
import java.util.HashMap;

public class ReliableBroadcast implements Registerable, Broadcast {
	ArrayList<String> delivered = new ArrayList<>();
	Parser parser;
	HashMap<Integer, PerfectLink> links;
	Broadcast upper = null;
	
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
    	delivered.add(Integer.toString(parser.myId()) + ":" + m);
    }
	
	public void deliver(int from, String m) {
		if(upper == null)
			Logger.log.add("d " + Integer.toString(from) + " " + m);
		else
			upper.deliver(from, m);
	}

	@Override
	public void handleMessage(int from, String m, String origin) {
		if(delivered.contains(origin + ":" + m))
			return;
		
	    new Thread(() -> linkSend(from, m, origin)).start();
		deliver(Integer.parseInt(origin), m);
		delivered.add(origin + ":" + m);
	}
	
	public void linkSend(int from, String m, String origin) {
		for(Host host : parser.hosts()) {
			if(host.getId() == from || host.getId() == Integer.parseInt(origin) || host.getId() == parser.myId())
				continue;
			PerfectLink link = links.get(host.getId());
			while(link == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					continue;
				}
				link = links.get(host.getId());
			}
			//PerfectLink l = link;
			synchronized(link) {
				link.send(m, origin);
				link.notify();
			}
			break;
		}
	}

	@Override
	public void registerBroadcast(Broadcast upper) {
		this.upper = upper;
	}
}
