package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PerfectLink implements Runnable {
	private DatagramSocket socket;
	private InetAddress distantIP;
	private int distantPort = 0;
	private int sequence = 0;
	private volatile int ack = -1;
	private boolean functional = true;
	private ConcurrentLinkedQueue<String> buffer = new ConcurrentLinkedQueue<>();

    public PerfectLink(DatagramSocket socket, String distantIP, int distantPort) throws UnknownHostException {
    	this.socket = socket;
    	this.distantIP = InetAddress.getByName(distantIP);
    	this.distantPort = distantPort;
    	functional = true;
    }
    
    public void kill() {
    	functional = false;
    }
    
    public void ack(int number) {
    	if(number > ack)
    		ack = number;
    }
    
    public boolean send(String m) {
		buffer.offer(m);
    	return functional;
    }

	@Override
	public void run() {
		while(functional) {
			String m = null;
			while(m == null) {
				synchronized(this) {
					m = buffer.poll();
					if(m == null) {
						try {
							this.wait(200);
						} catch (InterruptedException e) {
							functional = false;
						}
					}
				}
			}
	    	String message = m + "§" + Integer.toString(sequence);
	    	System.out.println("SENDING MESSAGE :" + message);
	    	System.out.println("TO " + distantIP.getHostAddress() + " : " + distantPort);
	    	
	    	try {
	    		DatagramPacket payload = new DatagramPacket(message.getBytes(), message.getBytes().length, distantIP, distantPort);
				boolean sent = false;
	    		for(int i = 0 ; i < 5 ; i++) {
					socket.send(payload);
					for(int j = 0 ; j < 5 ; j++) {
						synchronized(this) {
							if(ack < sequence)
						    	this.wait(200);
							else {
								sequence++;
								sent = true;
								i = 1000;
								j = 1000;
							}
						}
					}
				}
				functional = sent;
				if(!functional)
					System.out.println("LINK DOWN");
			} catch (IOException | InterruptedException e) {
				functional = false;
			}
		}
	}
}