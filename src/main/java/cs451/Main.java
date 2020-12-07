package cs451;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class Main {
	private static ReceiveDispatcher dispatcher;
	private static String outputPath;
	private static DatagramSocket socket;
	
    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        
        dispatcher.killThread();
        socket.close();

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
        
        //System.exit(0);
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
        socket = new DatagramSocket(myPort, myIP);
        
        HashMap<Integer, PerfectLink> links = new HashMap<>();
        for(int i = 0 ; i < parser.hosts().size() ; i++) {
        	if(parser.hosts().get(i).getId() == parser.myId())
        		continue;
        	PerfectLink link = new PerfectLink(socket, parser.hosts().get(i).getIp(), parser.hosts().get(i).getPort(), parser.myId());
        	links.put(parser.hosts().get(i).getId(), link);
        	Thread linkThread = new Thread(link);
        	linkThread.start();
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
        boolean[] dependsOn = new boolean[parser.hosts().size()];
        for(int i = 0 ; i < dependsOn.length ; i++)
        	dependsOn[i] = false;
        if (parser.hasConfig()) {
            System.out.println("Config: " + parser.config());
            
            BufferedReader reader = new BufferedReader(new FileReader(new File(parser.config())));
            numberOfMessages = Integer.parseInt(reader.readLine(), 10);
            for(int i = 0 ; i < dependsOn.length ; i++) {
            	String[] tokens = reader.readLine().split(" ");
            	if(Integer.parseInt(tokens[0].trim(), 10) != parser.myId())
            		continue;
            	for(int j = 1 ; j < tokens.length ; j++) {
            		if(tokens[j].trim().isEmpty())
            			continue;
            		int token = Integer.parseInt(tokens[j].trim(), 10);
            		dependsOn[token - 1] = true;
            	}
            }
            dependsOn[parser.myId() - 1] = true;
            reader.close();
        }
        
        System.out.println("DEPENDS ON ");
        for(int i = 0 ; i < dependsOn.length ; i++)
        	System.out.println(dependsOn[i]);

        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());
        CausalBroadcast engine = new CausalBroadcast(links, parser, numberOfMessages, dependsOn);
        dispatcher = new ReceiveDispatcher(socket, parser, links);
        dispatcher.registerHandler(engine);
        Thread dispatcherThread = new Thread(dispatcher);
        dispatcherThread.start();
        outputPath = parser.output();
        
	    System.out.println("Waiting for all processes for finish initialization");
        coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");
        System.out.println("Starting reliable broadcast of " + numberOfMessages + " messages");
        
        for(int i = 1 ; i <= numberOfMessages ; i++) {
        	//Thread.sleep(100);
        	engine.broadcast(Integer.toString(i));
        }
        
        System.out.println("Signaling end of broadcasting messages");
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
