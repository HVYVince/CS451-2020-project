package cs451;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
	private static ReceiveDispatcher dispatcher;
	private static String outputPath;
	
    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        
        dispatcher.killThread();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        try {
	        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputPath)));
	        for(int i = 0 ; i < Logger.log.size() ; i++) {
	        	writer.write(Logger.log.get(i));
	        	writer.newLine();
	        }
	        writer.close();
        } catch(Exception e) {
        	return;
        }
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws Exception {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID is " + pid + ".");
        System.out.println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        System.out.println("My id is " + parser.myId() + ".");

        InetAddress myIP = null;
        int myPort = 0;
        for(Host host : parser.hosts()) {
        	if(host.getId() == parser.myId()) {
        		myIP = InetAddress.getByName(host.getIp());
                myPort = host.getPort();
        	}
        }
        DatagramSocket socket = new DatagramSocket(myPort, myIP);
        
        HashMap<Integer, PerfectLink> links = new HashMap<>();
        for(int i = 0 ; i < parser.hosts().size() ; i++) {
        	if(parser.hosts().get(i).getId() == parser.myId())
        		continue;
        	links.put(parser.hosts().get(i).getId(), new PerfectLink(socket, parser.hosts().get(i).getIp(), parser.hosts().get(i).getPort()));
        }
        
        System.out.println("List of hosts is:");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId() + ", " + host.getIp() + ", " + host.getPort());
        }

        System.out.println("Barrier: " + parser.barrierIp() + ":" + parser.barrierPort());
        System.out.println("Signal: " + parser.signalIp() + ":" + parser.signalPort());
        System.out.println("Output: " + parser.output());
        // if config is defined; always check before parser.config()
        int numberOfMessages = 0;
        if (parser.hasConfig()) {
            System.out.println("Config: " + parser.config());
            
            BufferedReader reader = new BufferedReader(new FileReader(new File(parser.config())));
            numberOfMessages = Integer.parseInt(reader.readLine(), 10);
            reader.close();
        }

        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());
        dispatcher = new ReceiveDispatcher(socket, parser, links);
        Thread dispatcherThread = new Thread(dispatcher);
        dispatcherThread.start();
        outputPath = parser.output();
        
	    System.out.println("Waiting for all processes for finish initialization");
        coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");
        System.out.println("Starting best effort broadcast of " + numberOfMessages + " messages");
        for(int i = 0 ; i < numberOfMessages ; i++) {
        	for(int id : links.keySet()) {
        		if(id == parser.myId())
        			continue;
        		PerfectLink link = links.get(id);
        		if(!link.send("BROADCAST : " + Integer.toString(i)))
        			System.out.println("LINK " + id + " DOWN");
        	}
        }

        System.out.println("Signaling end of broadcasting messages");
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
