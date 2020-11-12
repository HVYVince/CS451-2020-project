package cs451;

import java.util.ArrayList;
import java.util.HashMap;

public class FIFOBroadcast implements Registerable, Broadcast {
	private ReliableBroadcast reliable;
	private ArrayList<HashMap<Integer, String>> receivedMessages;
	private int[] nextToDelivers;
	
	public FIFOBroadcast(HashMap<Integer, PerfectLink> links, Parser parser) {
		reliable = new ReliableBroadcast(links, parser);
		reliable.registerBroadcast(this);
		receivedMessages = new ArrayList<>();
		nextToDelivers = new int[parser.hosts().size()];
		for(int i = 0 ; i < parser.hosts().size() ; i++) {
			receivedMessages.add(new HashMap<>());
			nextToDelivers[i] = 1;
		}
	}

	@Override
	public void handleMessage(int from, String m, String origin) {
		reliable.handleMessage(from, m, origin);
	}

	@Override
	public void broadcast(String m) {
		reliable.broadcast(m);
	}

	@Override
	public void deliver(int from, String m) {
		HashMap<Integer, String> messages = receivedMessages.get(from - 1);
		messages.put(Integer.parseInt(m), "d " + Integer.toString(from) + " " + m);
		String message = messages.get(nextToDelivers[from - 1]);
		while(message != null) {
			Logger.log.add(message);
			nextToDelivers[from - 1]++;
			message = messages.get(nextToDelivers[from - 1]);
		}
	}

	@Override
	public void registerBroadcast(Broadcast upper) {
		return;
	}
}
