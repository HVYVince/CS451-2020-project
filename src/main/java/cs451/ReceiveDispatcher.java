package cs451;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;

public class ReceiveDispatcher implements Runnable {
	private DatagramSocket socket;
	private boolean run = true;
	private Parser parser;
	private HashMap<Integer, PerfectLink> links;
	private ArrayList<Registerable> registered;
	
	ReceiveDispatcher(DatagramSocket socket, Parser parser, HashMap<Integer, PerfectLink> links) {
		this.socket = socket;
		this.parser = parser;
		this.links = links;
	}
	
	void killThread() {
		run = false;
	}
	
	void registerHandler(Registerable registerable) {
		registered.add(registerable);
	}

	@Override
	public void run() {
		while(run) {
			byte[] buffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buffer, 1024);
			try {
		        socket.receive(packet);
		        System.out.println("RECEIVED : " + new String(packet.getData()));
		        System.out.println("FROM : " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
		        String[] tokens = (new String(packet.getData())).split("§");
		        if(tokens[0].contentEquals("ACK")) {
		        	for(Host host : parser.hosts()) {
		        		if(packet.getAddress().toString().substring(1).contentEquals(host.getIp()) && packet.getPort() == host.getPort()) {
		        			PerfectLink link = links.get(host.getId());
		        			synchronized(link) {
		        				link.ack(Integer.parseInt(tokens[1].trim()));
		        				link.notify();
		        			}
		        		}
		        	}
		        }
		        else {
		        	String m = "ACK§" + tokens[1];
			        DatagramPacket ack = new DatagramPacket(m.getBytes(), m.getBytes().length, packet.getAddress(), packet.getPort());
			        System.out.println("SENDING ACK : " + m);
			        System.out.println("TO : " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
			        socket.send(ack);
			        
			        for(Host host : parser.hosts()) {
			        	if(host.getIp().contentEquals(packet.getAddress().toString().substring(1)) && host.getPort() == packet.getPort()) {
					        for(Registerable reg : registered)
					        	reg.handleMessage(host.getId(), tokens[0].trim());
					        break;
			        	}
			        }
		        }
				Thread.sleep(100);
			} catch(Exception e) {
				return;	
			}
		}
	}
}
