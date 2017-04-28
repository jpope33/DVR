package DVR.DVRThread;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Router5002 { //NEW CLASS CHANGE

    private static Router5002 classInstance; //NEW CLASS CHANGE
    private static DatagramSocket socket;
    private InetAddress localHost;
    private static int hostPort = 5002; //NEW CLASS CHANGE

    private static String filename;
    //not sure if these need to be global, local in readAndSend for now
    /*
    private String neighbors[] = null; // array of neighbors
    private LinkedList<String[]> routingTable = new LinkedList<>(); // list of routing table entries
    private RouterNode routerNode;// router node object, composed of 'neighbors' array and 'routingTable' LL
    */


    public static void main(String[] args){

        classInstance = new Router5002(); //NEW CLASS CHANGE

        classInstance.setupConstants(args);

        new ListenAndUpdate5001(socket, filename, hostPort).start(); //NEW CLASS CHANGE

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                classInstance.readAndSend();
            }
        };
        timer.schedule(task, 10000, 10000);
    }






    private void setupConstants(String[] args) {
        //setup socket
        try {
            socket = new DatagramSocket(hostPort);
            localHost = InetAddress.getLocalHost();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //setup router I/O file
        filename = args[0];
    }






    private void readAndSend() {

        //not sure if these need to be global
        String neighbors[] = null; // array of neighbors
        LinkedList<String[]> routingTable = new LinkedList<>(); // list of routing table entries
        RouterNode routerNode = null;// router node object, composed of 'neighbors' array and 'routingTable' LL

        try {

            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            System.out.println();
            System.out.println("**********READ AND SEND BEGIN AT " + sdf.format(date));

            Scanner scanner = new Scanner(new File(filename));
            String content; // holds file input for parsing
            boolean neighborLine = true; // marks first file line which is a list of neighbor nodes
            String routingTableEntry[]; // individual routing table entry, (destination, cost, next hop)

            while (scanner.hasNextLine()){
                content = scanner.nextLine(); // reads individual line
                //System.out.println("Content: " + content);

                if (neighborLine){// first line is neighbor line
                    neighbors = content.split(",");
                    //System.out.println(Arrays.toString(neighbors) + " Added As Neighbor");
                    neighborLine = false;
                }else {// remaining lines are individual table entries
                    routingTableEntry = content.split(",");
                    routingTable.add(routingTableEntry);
                    //System.out.println(Arrays.toString(routingTableEntry) + " Added As RTE");
                }
            }

            routerNode = new RouterNode(neighbors, routingTable);
            System.out.println();
            System.out.println("Router Node Post Read Pre Neighbor Push:");
            routerNode.printNode();
            //routingTable = new LinkedList<>();// reset or carry over will occur

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Message Sent To Neighbors:");
        // cycles neighbors and sends them distance vector values
        for (int i = 0; i < routerNode.getNeighbors().length; i++){

            // simply converts neighbor entry from string to int, used as packet argument
            int neighborPort = Integer.parseInt(neighbors[i]);

            // this returns destination and cost of each entry in the routing table, then sends to neighbors
            String message = routerNode.getDistanceVectorValues(routerNode.getRoutingTable());
            System.out.println(neighborPort + " - " + message);
            byte messageByte[] = message.getBytes();
            DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, localHost, neighborPort);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        //System.out.println(sdf.format(date));
        System.out.println("At Time: " + sdf.format(date));
        System.out.println();
        //System.out.println("Router Node Post Neighbor Push:");
        //routerNode.printNode();
        System.out.println("**********READ AND SEND END");
    }
}






class ListenAndUpdate5002 extends Thread{ //NEW CLASS CHANGE

    private DatagramSocket socket;
    private String filename;
    private int hostPort;

    private byte[] receivedBytes = new byte[1024];
    private DatagramPacket receivedPacket;

    //not sure if these need to be global
    String neighbors[] = null; // array of neighbors
    LinkedList<String[]> routingTable = new LinkedList<>(); // list of routing table entries
    RouterNode routerNode = null;// router node object, composed of 'neighbors' array and 'routingTable' LL

    public ListenAndUpdate5002(DatagramSocket socket, String filename, int hostPort) { //NEW CLASS CHANGE
        this.socket = socket;
        this.filename = filename;
        this.hostPort = hostPort;
    }

    @Override
    public void run() {

        while (true){
            try {

                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                //System.out.println(sdf.format(date));
                System.out.println();
                System.out.println("**********LISTEN AND UPDATE BEGIN AT " + sdf.format(date));
                System.out.println();

                receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
                socket.receive(receivedPacket);
                String receivedString = new String(receivedPacket.getData());
                System.out.println("receivedString: " + receivedString);

                String senderPortString = String.valueOf(receivedPacket.getPort()); // used to identify redundant vector comparisons
                String hostPortString = String.valueOf(hostPort);

                String[] vectorValue = receivedString.split(",");
                //System.out.println("vectorValue: " +);

                // holds only pertinent vector values., i.e. not sender or self
                LinkedList<String> vectorValueLean = new LinkedList<>();

                System.out.println();
                System.out.println("BUILDING LEAN FROM SENDER " + receivedPacket.getPort());
                for (int i = 0; i < vectorValue.length; i++){
                    //System.out.print("Vector Value - " + vectorValue[i]);

                    // split and set destination and cost variables
                    String tempString = vectorValue[i];
                    String[] tempArray = tempString.split(":");
                    String destination = tempArray[0];
                    String cost = tempArray[1];

                    //System.out.println("Destination: " + destination);
                    //System.out.println("Cost " + cost);

                    // if destination isn't the sender or host, add to lean
                    if (destination.equals(senderPortString) || destination.equals(hostPortString)){
                        //System.out.println(" Redundant, Not Added");
                    }else {
                        vectorValueLean.add(destination + ":" + cost);
                        //System.out.println(" Added To Lean");
                    }

                    //System.out.println("tempArray- " + Arrays.toString(tempArray));
                }

                // read and make node object for comparision
                Scanner scanner = new Scanner(new File(filename));
                String content; // holds file input for parsing
                boolean neighborLine = true; // marks first file line which is a list of neighbor nodes
                String routingTableEntry[]; // individual routing table entry, (destination, cost, next hop)

                //System.out.println();
                while (scanner.hasNextLine()){
                    content = scanner.nextLine(); // reads individual line
                    //System.out.println("Content: " + content);

                    // this is keep for writing back to file later
                    if (neighborLine){// first line is neighbor line
                        neighbors = content.split(",");
                        //System.out.println(Arrays.toString(neighbors) + " Added As Neighbor");
                        neighborLine = false;
                    }else {// table entries which are analyzed and possibly updated with dvr algo
                        routingTableEntry = content.split(",");
                        routingTable.add(routingTableEntry);
                        //System.out.println(Arrays.toString(routingTableEntry) + " Added As RTE");
                    }
                }
                routerNode = new RouterNode(neighbors, routingTable);
                //System.out.println();
                //System.out.println("Router Node Post Read, Used In Subsequent Comparisons");
                //routerNode.printNode();

                // perform route comparision calculations and update routing table in routerNode object if needed
                boolean updateNeed = false; // flag for file re-write
                System.out.println();
                System.out.println("UPDATE CALCULATIONS");
                for (int i =0; i < vectorValueLean.size(); i++){

                    String[] temp = vectorValueLean.get(i).split(":");

                    // this is how 'fast' the host node can get to the sending node
                    String currentCostToSendingNode = routerNode.getCost(senderPortString);

                    // this is how 'fast' the sending node can get to the destination in question
                    String tempDestination = temp[0].trim();
                    String tempCost = temp[1].trim();

                    System.out.println("Temp [dest, cost]: " + Arrays.toString(temp));
                    //System.out.println("tempDestination- " + tempDestination);
                    //System.out.println("tempCost- " + tempCost);

                    // this is how 'fast' the host node can currently get to the same destination
                    String currentCostToTempDestination = routerNode.getCost(tempDestination);

                    /*
                    If the cost from 5004 to the sending node plus the cost from the sending node to
                    the destination in question is < the current cost from 5004 to the destination,
                    update 5004 routing table by updating routerNode object. Then use routerNode as template
                    for new file.
                    */

                    //System.out.println("COMPARISION");
                    System.out.println("Is " + currentCostToSendingNode + " + " + tempCost + " < " + currentCostToTempDestination + "?");
                    int cctsn = Integer.parseInt(currentCostToSendingNode);
                    int tc = Integer.parseInt(tempCost);
                    int ccttd = Integer.parseInt(currentCostToTempDestination);

                    if (cctsn + tc < ccttd){
                        System.out.println();
                        System.out.println("!!!!!!!!!!!!!!!!!!!!! UPDATE EVENT BEGIN");
                        //System.out.println("Sender: " + senderPortString);
                        //System.out.println("Destination: " + tempDestination);
                        System.out.println("Path from " + hostPort + " to " + tempDestination + " updated via " + receivedPacket.getPort());
                        updateNeed = true;

                        System.out.println();
                        System.out.println("Node pre update");
                        routerNode.printNode();

                        //update with [tempDestination, cctsn + tc, sendingNode]
                        String[] newDistanceVectorEntry = {tempDestination, String.valueOf(cctsn+tc), senderPortString};
                        System.out.println("New DVE: " + Arrays.toString(newDistanceVectorEntry));

                        routerNode.updateDistanceVectorEntry(newDistanceVectorEntry);

                        System.out.println();
                        System.out.println("Node post update");
                        routerNode.printNode();
                    }
                }

                if (updateNeed){

                    // write file back
                    //System.out.println();
                    System.out.println("WRITING BACK");
                    File file = new File(filename);
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));

                    //System.out.println("Node pre write");
                    //routerNode.printNode();

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

                        //prevents adding extra blank line
                        if ( i != routingTable.size() - 1){
                            bufferedWriter.write(sbTableEntry.toString());
                            bufferedWriter.newLine();
                        }else {
                            bufferedWriter.write(sbTableEntry.toString());
                        }
                    }
                    bufferedWriter.close();
                }

                routingTable = new LinkedList<>();// reset or carry over will occur

                System.out.println();
                System.out.println("**********LISTEN AND UPDATE END");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}