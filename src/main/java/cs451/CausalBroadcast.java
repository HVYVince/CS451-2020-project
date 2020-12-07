package cs451;

import java.util.ArrayList;
import java.util.HashMap;

public class CausalBroadcast implements Registerable, Broadcast {
	private int[] vectorClock;
	private boolean[] dependency;
	private int id;
	private int numberOfHosts;
	private HashMap<Integer, PerfectLink> links;
	private ArrayList<HashMap<Integer, String>> receivedMessages;
	private ReliableBroadcast reliable;
	private int totalMessageCount;
	//private int broadcasted;
	
	public CausalBroadcast(HashMap<Integer, PerfectLink> links, Parser parser, int total, boolean[] dependancy) {
		id = parser.myId() - 1;
		numberOfHosts = parser.hosts().size();
		this.links = links;
		this.dependency = dependancy;
		reliable = new ReliableBroadcast(links, parser);
		reliable.registerBroadcast(this);
		totalMessageCount = total;
		receivedMessages = new ArrayList<>();
		vectorClock = new int[parser.hosts().size()];
		
		for(int i = 0 ; i < parser.hosts().size() ; i++) {
			receivedMessages.add(new HashMap<>());
			vectorClock[i] = 0;
		}
	}

	public void broadcast(String m) {
		String clockRequirement = "";
		synchronized(vectorClock) {
			for(int i = 0 ; i < dependency.length ; i++) {
				if(dependency[i])
					clockRequirement += Integer.toString(vectorClock[i]);
				else
					clockRequirement += "0";
				if(i < dependency.length - 1)
					clockRequirement += "~";
			}

			Logger.log.add("b " + m);
		}
		reliable.broadcast(m + "~" + clockRequirement);	
	}

	public void deliver(int from, String m) {
		String[] receivedToken = m.split("~");
		HashMap<Integer, String> messages = receivedMessages.get(from - 1);
		messages.put(Integer.parseInt(receivedToken[0].trim()), m);
		
		synchronized (vectorClock) {
			boolean hasDelivered = true;
			while(hasDelivered) {
				hasDelivered = false;
				for(int i = 0 ; i < receivedMessages.size() ; i++) {
					HashMap<Integer, String> stored = receivedMessages.get(i);
					String message = stored.get(vectorClock[i] + 1);
					while(message != null) {
						String[] requiredTokens = m.split("~");
						int[] requiredClock = new int[requiredTokens.length - 1];
						int sequence = Integer.parseInt(requiredTokens[0].trim());
						for(int j = 1 ; j < requiredTokens.length ; j++)
							requiredClock[j - 1] = Integer.parseInt(requiredTokens[j].trim());
						
						boolean clockOk = true;
						for(int j = 0 ; j < numberOfHosts ; j++)
							if(vectorClock[j] < requiredClock[j])
								clockOk = false;
						if(clockOk) {
							vectorClock[i]++;
							Logger.log.add("d " + (i + 1) + " " + sequence);
							hasDelivered = true;
						}
						message = stored.get(vectorClock[i] + 1);
					}
				}
			}
		}
	}

	public void registerBroadcast(Broadcast upper) {
		return;
	}

	public void handleMessage(int from, String m, String origin) {
		reliable.handleMessage(from, m, origin);
	}
}
