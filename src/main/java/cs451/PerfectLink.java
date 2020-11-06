package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Base64;

public class PerfectLink {
	private DatagramSocket socket;
	private InetAddress distantIP;
	private int distantPort = 0;
	private int sequence = 0;
	private volatile int ack = -1;
	private boolean functional = true;

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
    	if(!functional)
    		return false;
    	
    	String message = m + "§" + Integer.toString(sequence);
    	System.out.println("SENDING MESSAGE :" + message);
    	
    	try {
    		DatagramPacket payload = new DatagramPacket(message.getBytes(), message.getBytes().length, distantIP, distantPort);
			for(int i = 0 ; i < 5 ; i++) {
				socket.send(payload);
				for(int j = 0 ; j < 5 ; j++) {
					synchronized(this) {
						System.out.println("ACK " + ack);
						if(ack < sequence)
					    	this.wait(200);
						else {
							sequence++;
							return true;
						}
					}
				}
			}
			functional = false;
		} catch (IOException | InterruptedException e) {
			functional = false;
		}
		
    	return functional;
    }
}