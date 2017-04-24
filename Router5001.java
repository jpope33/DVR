//package DVR;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Scanner;

public class Router5001 {

    private static Router5001 classInstance;

    private DatagramSocket socket;
    private InetAddress localhost;
    private int hostPort = 5001;

    private String filename;// reference to routerTable.txt file
    private String neighbors[] = null; // array of neighbor nodes
    private LinkedList<String[]> routingTable = new LinkedList(); // list of routing table entries
    private RouterNode routerNode;// router node object, composed of 'neighbors' array and 'routingTable' LL

    //private byte[] reveivedBytes = new byte[1024];
    private DatagramPacket packet;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Argument Error");
        } else {
            classInstance = new Router5001();
            classInstance.setupConstants(args);
            classInstance.readAndSend();// initial read and send call
            classInstance.listenAndUpdate();// recursive call, calls readAndSend()
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
    }

    private void readAndSend() {

        try {

            Thread.sleep(10000); // 10 second pause before read and send

            System.out.println();
            System.out.println("**********READ AND SEND BEGIN");
            Scanner scanner = new Scanner(new File(filename));
            String content; // holds file input for parsing
            boolean neighborLine = true; // marks first file line which is a list of neighbor nodes
            String routingTableEntry[]; // individual routing table entry, (destination, cost, next hop)

            while (scanner.hasNextLine()){
                content = scanner.nextLine(); // reads individual line

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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // cycles neighbors and sends them distance vector values
        for (int i = 0; i < routerNode.getNeighbors().length; i++){

            // simply converts neighbor entry from string to int, used as packet argument
            int port = Integer.parseInt(neighbors[i]);

            // this returns destination and cost of each entry in the routing table, then sends to neighbors
            String message = routerNode.getDistanceVectorValues(routerNode.getRoutingTable());
            byte messageByte[] = message.getBytes();
            DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, localhost, port);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("**********READ AND SEND END");
    }

    private void listenAndUpdate() {

        while (true){
            try {
            	byte[] reveivedBytes = new byte[1024];
                packet = new DatagramPacket(reveivedBytes, reveivedBytes.length);// received packet
                socket.receive(packet);
                String receivedString = new String(packet.getData());

                System.out.println();
                System.out.println("**********LISTEN AND UPDATE BEGIN");

                String senderPortString = String.valueOf(packet.getPort()); // used to identify redundant vector comparisons
                String hostPortString = String.valueOf(hostPort);// used to identify redundant vector comparisons

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
                    }
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

                classInstance.readAndSend();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}