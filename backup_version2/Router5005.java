//package DVR;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Router5005 {

    private static Router5005 classInstance;

    private DatagramSocket socket;
    private InetAddress localhost;
    private int hostPort = 5005;

    private static String filename;// reference to routerTable.txt file
    private static String initialFile;
    //private static String currentFileContent;
    private static String initialFileContent;
    private static boolean changed = false;
    
    private String neighbors[] = null; // array of neighbor nodes
    private LinkedList<String[]> routingTable = new LinkedList(); // list of routing table entries
    private RouterNode routerNode;// router node object, composed of 'neighbors' array and 'routingTable' LL

    //private byte[] reveivedBytes = new byte[1024];
    private DatagramPacket packet;
    Thread t1;

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Argument Error");
        } else {
        	try {
				Thread.sleep(30000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            classInstance = new Router5005();
            classInstance.setupConstants(args);
            classInstance.readAndSend(changed, null,null);// initial read and send call
            
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
				
				@Override
				public void run() {
					System.out.println("\n\n\n************************************Timeout**************\n\n\n\n");
					Scanner scanner ;
			        String content; // holds file input for parsing
			        boolean neighborLine = true; // marks first file line which is a list of neighbor nodes
			        String routingTableEntry[];
			        String currentFileContent = "";
					try{
					 	scanner = new Scanner(new File(initialFile));
				         // individual routing table entry, (destination, cost, next hop)
				        System.out.println("Timeout");
				        while (scanner.hasNextLine()){
				        	currentFileContent = currentFileContent+"\n"+scanner.nextLine(); // reads individual line
				        }
				        if(!currentFileContent.equalsIgnoreCase(initialFileContent))
				        {
				        	System.out.println("File Changed : "+currentFileContent);
				        	initialFileContent = currentFileContent;
				        	File file = new File(filename);
		                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));

		                    // write neighbors line
		                    bufferedWriter.write(initialFileContent.trim().toString());
		                    bufferedWriter.newLine();
		                    bufferedWriter.close();
		                    changed = true;
		                    classInstance.readAndSend(changed,null,null);
		                    
				        }
				        
				        
					}catch(Exception e)
					{
						System.out.println("File not found"+e);
					}
				}
			};
			timer.schedule(task, 30000, 30000);
			classInstance.callThread();// recursive call, calls readAndSend()
			classInstance.readAndSend(changed,null,null);
        }
    }

    private void setupConstants(String[] args) {

        //setup socket
        try {
            socket = new DatagramSocket(hostPort);
            localhost = InetAddress.getLocalHost();
            
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //setup router I/O file
        filename = args[0];
        initialFile = args[1];
        
        Scanner scanner;
		try {
			scanner = new Scanner(new File(initialFile));
		
	        String content; // holds file input for parsing
	        boolean neighborLine = true; // marks first file line which is a list of neighbor nodes
	        String routingTableEntry[]; // individual routing table entry, (destination, cost, next hop)
	        initialFileContent="";
	        while (scanner.hasNextLine()){
	        	initialFileContent=initialFileContent+"\n"+scanner.nextLine(); // reads individual line
	        }
	        System.out.println("Initial File : "+initialFileContent);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("File Not found"+e);
		}
        
    }

    private void readAndSend(boolean flag, String senderport,String portsSent) {

        try {

            //Thread.sleep(10000); // 10 second pause before read and send

            System.out.println();
            System.out.println("**********READ AND SEND BEGIN");
            Scanner scanner = new Scanner(new File(filename));
            String content; // holds file input for parsing
            boolean neighborLine = true; // marks first file line which is a list of neighbor nodes
            String routingTableEntry[]; // individual routing table entry, (destination, cost, next hop)

            while (scanner.hasNextLine()){
                content = scanner.nextLine(); // reads individual line
                System.out.println("Contents : "+content);

                if (neighborLine){// first line is neighbor line
                    neighbors = content.split(",");
                    neighborLine = false;
                }else {// remaining lines are individual table entries
                    routingTableEntry = content.split(",");
                    routingTable.add(routingTableEntry);
                }
            }
            
            routerNode = new RouterNode(neighbors, routingTable);
            routingTable = new LinkedList<>();// reset or carry over will occur

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }/* catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        ArrayList<String> neig;
        // cycles neighbors and sends them distance vector values
        if(changed){
        	
        	String portsToSend= "";
        	if(portsSent!=null && !portsSent.isEmpty()){
        		portsToSend = portsSent;
	        	String[] ports = portsSent.split(":");
	        	
	        	neig = new ArrayList<String>();
	        	boolean flag1 = false;
	        	for(String n : neighbors){
	        		flag1 = false;
	        		for(String p : ports){
	        			if(p.equals(n)){
	        				flag1 = true;
	        			}
	            	}
	        		if(flag){
	        			neig.add(n);
	        		}
	        	}
        	}
        	else{
        		neig = new ArrayList<String>();
	        	boolean flag1 = false;
	        	for(String n : neighbors){
	        		neig.add(n);
	        	}
        	}
        	for(int i=0;i< neig.size();i++){
        		if(portsToSend.isEmpty()){
        		portsToSend=neig.get(i);
        		}
        		else{
        			portsToSend=portsToSend+":"+neig.get(i);
        		}
        	}
        	for(int i=0;i< neig.size();i++){
	        	String message = String.valueOf(changed)+"//"+portsToSend.toString();
	            byte messageByte[] = message.getBytes();
	            DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, localhost, Integer.parseInt(neig.get(i)));
	            try {
	                socket.send(packet);
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
        	}
        	changed = false;
        	
        	Timer timer = new Timer();
            TimerTask task1 = new TimerTask() {
				
				@Override
				public void run() {
					for (int i = 0; i < routerNode.getNeighbors().length; i++){
						
			            // simply converts neighbor entry from string to int, used as packet argument
			        	
				            int port = Integer.parseInt(neighbors[i]);
				
				            // this returns destination and cost of each entry in the routing table, then sends to neighbors
				            String routingTable = routerNode.getDistanceVectorValues(routerNode.getRoutingTable());
				            changed=false;
				            String message = String.valueOf(changed)+"//"+routingTable.toString();
				            byte messageByte[] = message.getBytes();
				            DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, localhost, port);
				            try {
				                socket.send(packet);
				            } catch (IOException e) {
				                e.printStackTrace();
				            }
			        }
					
				}
			};
			timer.schedule(task1, 60000);
        	
        }else{
	        for (int i = 0; i < routerNode.getNeighbors().length; i++){
	
	            // simply converts neighbor entry from string to int, used as packet argument
	        	
		            int port = Integer.parseInt(neighbors[i]);
		
		            // this returns destination and cost of each entry in the routing table, then sends to neighbors
		            String routingTable = routerNode.getDistanceVectorValues(routerNode.getRoutingTable());
		            String message = String.valueOf(changed)+"//"+routingTable.toString();
		            byte messageByte[] = message.getBytes();
		            DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, localhost, port);
		            try {
		                socket.send(packet);
		            } catch (IOException e) {
		                e.printStackTrace();
		            }
	        }
        }
        System.out.println("**********READ AND SEND END");
    }
    
    private void callThread() {
		
    	Reciever myServer = new Reciever();
    	t1 = new Thread(myServer, "T1");
        t1.start();
	}

    class Reciever implements Runnable{

		@Override
		public void run() {
			 while (true){
		            try {
		            	byte[] reveivedBytes = new byte[1024];
		                packet = new DatagramPacket(reveivedBytes, reveivedBytes.length);// received packet
		                socket.receive(packet);
		                String senderPortString = String.valueOf(packet.getPort()); // used to identify redundant vector comparisons
		                String hostPortString = String.valueOf(hostPort);// used to identify redundant vector comparisons
		                String fullData = new String(packet.getData());
		                String hasChanged = fullData.split("//")[0];
		                
		                if(hasChanged.equals(String.valueOf((boolean) true))){
		                	File file = new File(filename);
		                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));

		                    // write neighbors line
		                    bufferedWriter.write(initialFileContent.toString().trim());
		                    bufferedWriter.newLine();
		                    bufferedWriter.close();
		                    changed = true;
		                    classInstance.readAndSend(changed, senderPortString,fullData.split("//")[1]);
		                    break;
		                }else if (!changed){
		                	String receivedString = fullData.split("//")[1];
		                System.out.println();
		                System.out.println("**********LISTEN AND UPDATE BEGIN");

		              

		                //System.out.println("Sender - Host: " + senderPortString + " - " + hostPortString);

		                String[] vectorValue = receivedString.split(",");
		                //System.out.println("vectorValue: " +);
		                // holds only pertinent vector values., i.e. not sender or self
		                LinkedList<String> vectorValueLean = new LinkedList<>();

		                System.out.println();
		                System.out.println("BUILDING LEAN FROM SENDER " + packet.getPort());// Lean is LL with pertinent values only
		                for (int i = 0; i < vectorValue.length; i++){
		                    System.out.print("Vector Value - " + vectorValue[i]);

		                    // split and set destination and cost variables
		                    // refer to formatting from getDistanceVectorValues
		                    String tempString = vectorValue[i];
		                    String[] tempArray = tempString.split(":");
		                    String destination = tempArray[0];
		                    String cost = tempArray[1];
		                    String nexthop = tempArray[2];
		                    //System.out.println("Destination: " + destination);
		                    //System.out.println("Cost " + cost);

		                    // if destination isn't the sender or host, add to lean
		                    /*if (destination.equals(senderPortString) || destination.equals(hostPortString)){
		                        System.out.println(" Redundant, Not Added To Lean");
		                    }else {*/
		                        vectorValueLean.add(destination + ":" + cost+":"+nexthop);
		                        System.out.println(" Added To Lean");
		                    //}
		                }

		                // read and make node object for comparision
		                Scanner scanner = new Scanner(new File(filename));
		                String content; // holds file input for parsing
		                boolean neighborLine = true; // marks first file line which is a list of neighbor nodes
		                String routingTableEntry[]; // individual routing table entry, (destination, cost, next hop)

		                while (scanner.hasNextLine()){
		                    content = scanner.nextLine(); // reads individual line

		                    // this is keep for writing back to file later
		                    if (neighborLine){// first line is neighbor line
		                        neighbors = content.split(",");
		                        neighborLine = false;
		                    }else {// table entries which are analyzed and possibly updated with dvr algo
		                        routingTableEntry = content.split(",");
		                        routingTable.add(routingTableEntry);
		                    }
		                }
		                routerNode = new RouterNode(neighbors, routingTable);

		                // perform route comparision calculations and update routing table in routerNode object if needed
		                boolean updateNeed = false; // flag for file re-write
		                System.out.println();
		                System.out.println("UPDATE CALCULATIONS");
		                for (int i =0; i < vectorValueLean.size(); i++){
		                    String[] temp = vectorValueLean.get(i).split(":");

		                    // this is how 'fast' the host node can get to the sending node
		                    String costAndNextHop = routerNode.getCost(senderPortString);
		                		  
		                    String currentCostToSendingNode = costAndNextHop.split(":")[0];
		                    String currentNexthopToSendingNode = costAndNextHop.split(":")[1];
		                    // this is how 'fast' the sending node can get to the destination in question
		                    String tempDestination = temp[0];
		                    String tempCost = temp[1].trim();
		                    String tempNexHtop = temp[2].trim();

		                    // this is how 'fast' the host node can currently get to the same destination
		                    String currentCostToTempDestination = routerNode.getCost(tempDestination).split(":")[0];

		                    /*
		                    If the cost from 5002 to the sending node plus the cost from the sending node to
		                    the destination in question is < the current cost from 5002 to the destination,
		                    update 5002 routing table by updating routerNode object. Then use routerNode as template
		                    for new file.
		                    */

		                    //System.out.println("COMPARISION");
		                    System.out.println("Is " + currentCostToSendingNode + " + " + tempCost + " < " + currentCostToTempDestination + "?");
		                    int cctsn = Integer.parseInt(currentCostToSendingNode);
		                    int tc = Integer.parseInt(tempCost);
		                    int ccttd = Integer.parseInt(currentCostToTempDestination);

		                    if (cctsn + tc < ccttd){
		                        updateNeed = true;

		                        //update with [tempDestination, cctsn + tc, sendingNode]
		                        String[] newDistanceVectorEntry = {tempDestination, String.valueOf(cctsn+tc), currentNexthopToSendingNode};
		                        routerNode.updateDistanceVectorEntry(newDistanceVectorEntry);
		                    }
		                }

		                if (updateNeed){
		                    // write file back
		                    System.out.println();
		                    System.out.println("WRITING BACK");
		                    File file = new File(filename);
		                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));

		                    // write neighbors line
		                    neighbors = routerNode.getNeighbors();
		                    StringBuilder sbNeighbors = new StringBuilder();
		                    for (int i = 0; i < neighbors.length; i++){
		                        if (i != neighbors.length - 1){
		                            sbNeighbors.append(neighbors[i] + ",");
		                        }else {
		                            sbNeighbors.append(neighbors[i]);
		                        }
		                    }
		                    bufferedWriter.write(sbNeighbors.toString());
		                    bufferedWriter.newLine();

		                    // write routing table
		                    // CONCERN: this may be the problem area
		                    routingTable = routerNode.getRoutingTable();
		                    for (int i = 0; i < routingTable.size(); i++){
		                        StringBuilder sbTableEntry = new StringBuilder();
		                        String[] temp = routingTable.get(i);
		                        for (int j = 0; j < temp.length; j++){
		                            if (j != temp.length - 1){
		                                sbTableEntry.append(temp[j] + ",");
		                            }else {
		                                sbTableEntry.append(temp[j]);
		                            }
		                        }
		                        bufferedWriter.write(sbTableEntry.toString());
		                        bufferedWriter.newLine();
		                    }
		                    bufferedWriter.close();
		                    classInstance.readAndSend(changed,null,null);
		                }

		                routingTable = new LinkedList<>();// reset or carry over will occur
		                updateNeed = false;
		                System.out.println();
		                System.out.println("**********LISTEN AND UPDATE END");

		               

		                } 
		            }catch (IOException e) {
		                e.printStackTrace();
		            }
		            
		        }
			
		}
    	
    	
    }
    /*private void listenAndUpdate() {
    	
        while (!changed){
            try {
            	byte[] reveivedBytes = new byte[1024];
                packet = new DatagramPacket(reveivedBytes, reveivedBytes.length);// received packet
                socket.receive(packet);
                String senderPortString = String.valueOf(packet.getPort()); // used to identify redundant vector comparisons
                String hostPortString = String.valueOf(hostPort);// used to identify redundant vector comparisons
                String fullData = new String(packet.getData());
                String hasChanged = fullData.split("//")[0];
                
                if(hasChanged.equals(String.valueOf((boolean) true))){
                	File file = new File(filename);
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));

                    // write neighbors line
                    bufferedWriter.write(initialFileContent.toString().trim());
                    bufferedWriter.newLine();
                    bufferedWriter.close();
                    changed = true;
                    classInstance.readAndSend(changed, senderPortString,fullData.split("//")[1]);
                    break;
                }else{
                	String receivedString = fullData.split("//")[1];
                System.out.println();
                System.out.println("**********LISTEN AND UPDATE BEGIN");

              

                //System.out.println("Sender - Host: " + senderPortString + " - " + hostPortString);

                String[] vectorValue = receivedString.split(",");
                //System.out.println("vectorValue: " +);
                // holds only pertinent vector values., i.e. not sender or self
                LinkedList<String> vectorValueLean = new LinkedList<>();

                System.out.println();
                System.out.println("BUILDING LEAN FROM SENDER " + packet.getPort());// Lean is LL with pertinent values only
                for (int i = 0; i < vectorValue.length; i++){
                    System.out.print("Vector Value - " + vectorValue[i]);

                    // split and set destination and cost variables
                    // refer to formatting from getDistanceVectorValues
                    String tempString = vectorValue[i];
                    String[] tempArray = tempString.split(":");
                    String destination = tempArray[0];
                    String cost = tempArray[1];
                    String nexthop = tempArray[2];
                    //System.out.println("Destination: " + destination);
                    //System.out.println("Cost " + cost);

                    // if destination isn't the sender or host, add to lean
                    if (destination.equals(senderPortString) || destination.equals(hostPortString)){
                        System.out.println(" Redundant, Not Added To Lean");
                    }else {
                        vectorValueLean.add(destination + ":" + cost+":"+nexthop);
                        System.out.println(" Added To Lean");
                    //}
                }

                // read and make node object for comparision
                Scanner scanner = new Scanner(new File(filename));
                String content; // holds file input for parsing
                boolean neighborLine = true; // marks first file line which is a list of neighbor nodes
                String routingTableEntry[]; // individual routing table entry, (destination, cost, next hop)

                while (scanner.hasNextLine()){
                    content = scanner.nextLine(); // reads individual line

                    // this is keep for writing back to file later
                    if (neighborLine){// first line is neighbor line
                        neighbors = content.split(",");
                        neighborLine = false;
                    }else {// table entries which are analyzed and possibly updated with dvr algo
                        routingTableEntry = content.split(",");
                        routingTable.add(routingTableEntry);
                    }
                }
                routerNode = new RouterNode(neighbors, routingTable);

                // perform route comparision calculations and update routing table in routerNode object if needed
                boolean updateNeed = false; // flag for file re-write
                System.out.println();
                System.out.println("UPDATE CALCULATIONS");
                for (int i =0; i < vectorValueLean.size(); i++){
                    String[] temp = vectorValueLean.get(i).split(":");

                    // this is how 'fast' the host node can get to the sending node
                    String costAndNextHop = routerNode.getCost(senderPortString);
                		  
                    String currentCostToSendingNode = costAndNextHop.split(":")[0];
                    String currentNexthopToSendingNode = costAndNextHop.split(":")[1];
                    // this is how 'fast' the sending node can get to the destination in question
                    String tempDestination = temp[0];
                    String tempCost = temp[1].trim();
                    String tempNexHtop = temp[2].trim();

                    // this is how 'fast' the host node can currently get to the same destination
                    String currentCostToTempDestination = routerNode.getCost(tempDestination).split(":")[0];

                    
                    If the cost from 5002 to the sending node plus the cost from the sending node to
                    the destination in question is < the current cost from 5002 to the destination,
                    update 5002 routing table by updating routerNode object. Then use routerNode as template
                    for new file.
                    

                    //System.out.println("COMPARISION");
                    System.out.println("Is " + currentCostToSendingNode + " + " + tempCost + " < " + currentCostToTempDestination + "?");
                    int cctsn = Integer.parseInt(currentCostToSendingNode);
                    int tc = Integer.parseInt(tempCost);
                    int ccttd = Integer.parseInt(currentCostToTempDestination);

                    if (cctsn + tc <= ccttd){
                        updateNeed = true;

                        //update with [tempDestination, cctsn + tc, sendingNode]
                        String[] newDistanceVectorEntry = {tempDestination, String.valueOf(cctsn+tc), currentNexthopToSendingNode};
                        routerNode.updateDistanceVectorEntry(newDistanceVectorEntry);
                    }
                }

                if (updateNeed){
                    // write file back
                    System.out.println();
                    System.out.println("WRITING BACK");
                    File file = new File(filename);
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));

                    // write neighbors line
                    neighbors = routerNode.getNeighbors();
                    StringBuilder sbNeighbors = new StringBuilder();
                    for (int i = 0; i < neighbors.length; i++){
                        if (i != neighbors.length - 1){
                            sbNeighbors.append(neighbors[i] + ",");
                        }else {
                            sbNeighbors.append(neighbors[i]);
                        }
                    }
                    bufferedWriter.write(sbNeighbors.toString());
                    bufferedWriter.newLine();

                    // write routing table
                    // CONCERN: this may be the problem area
                    routingTable = routerNode.getRoutingTable();
                    for (int i = 0; i < routingTable.size(); i++){
                        StringBuilder sbTableEntry = new StringBuilder();
                        String[] temp = routingTable.get(i);
                        for (int j = 0; j < temp.length; j++){
                            if (j != temp.length - 1){
                                sbTableEntry.append(temp[j] + ",");
                            }else {
                                sbTableEntry.append(temp[j]);
                            }
                        }
                        bufferedWriter.write(sbTableEntry.toString());
                        bufferedWriter.newLine();
                    }
                    bufferedWriter.close();
                }

                routingTable = new LinkedList<>();// reset or carry over will occur

                System.out.println();
                System.out.println("**********LISTEN AND UPDATE END");

                classInstance.readAndSend(changed,null,null);

                } 
            }catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }*/
}