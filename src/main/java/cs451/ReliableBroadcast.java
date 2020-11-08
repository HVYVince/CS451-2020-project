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
	public void handleMessage(int from, String m) {
		if(delivered.contains(Integer.toString(from) + ":" + m))
			return;
		
	    new Thread(() -> linkSend(from, m)).start();
		deliver(from, m);
		delivered.add(Integer.toString(from) + ":" + m);
	}
	
	public void linkSend(int from, String m) {
		for(Host host : parser.hosts()) {
			if(host.getId() == from || host.getId() == parser.myId()) {
				PerfectLink link = links.get(host.getId());
				while(link == null) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						continue;
					}
					link = links.get(host.getId());
				}
				PerfectLink l = link;
				synchronized(l) {
					l.send(m);
					l.notify();
				}
				break;
			}
		}
	}

	@Override
	public void registerBroadcast(Broadcast upper) {
		this.upper = upper;
	}
}
