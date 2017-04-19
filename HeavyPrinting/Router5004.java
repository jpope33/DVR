
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;

public class Router5004 {//NEW CLASS CHANGE

    private static Router5004 classInstance;//NEW CLASS CHANGE

    private DatagramSocket socket;
    private InetAddress localhost;
    private int hostPort = 5004;//NEW CLASS CHANGE

    private String filename;
    private String neighbors[] = null; // array of neighbors
    private LinkedList<String[]> routingTable = new LinkedList<>(); // list of routing table entries
    private RouterNode routerNode;// router node object, composed of 'neighbors' array and 'routingTable' LL

    private byte[] reveivedBytes = new byte[1024];
    private DatagramPacket packet;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Argument Error");
        } else {
            classInstance = new Router5004();//NEW CLASS CHANGE
            classInstance.setupConstants(args);
            classInstance.readAndSend(); // initial read and send call
            classInstance.listenAndUpdate(); // recursive call, calls readAndSend()
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
                System.out.println("Content: " + content);

                if (neighborLine){// first line is neighbor line
                    neighbors = content.split(",");
                    System.out.println(Arrays.toString(neighbors) + " Added As Neighbor");
                    neighborLine = false;
                }else {// remaining lines are individual table entries
                    routingTableEntry = content.split(",");
                    routingTable.add(routingTableEntry);
                    System.out.println(Arrays.toString(routingTableEntry) + " Added As RTE");
                }
            }

            routerNode = new RouterNode(neighbors, routingTable);
            System.out.println();
            System.out.println("Router Node Post Read:");
            //System.out.println(routerNode.printNode());
            routerNode.printNode();
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
            System.out.println("Message: " + message);
            byte messageByte[] = message.getBytes();
            DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, localhost, port);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println();
        System.out.println("Router Node Neighbor Push:");
        //System.out.println(routerNode.printNode());
        routerNode.printNode();
        System.out.println("**********READ AND SEND END");
    }

    private void listenAndUpdate() {

        while (true){
            try {

                packet = new DatagramPacket(reveivedBytes, reveivedBytes.length);
                socket.receive(packet);
                String receivedString = new String(packet.getData());

                System.out.println();
                System.out.println("**********LISTEN AND UPDATE BEGIN");

                String senderPortString = String.valueOf(packet.getPort()); // used to identify redundant vector comparisons
                String hostPortString = String.valueOf(hostPort);

                //System.out.println("Sender - Host: " + senderPortString + " - " + hostPortString);

                String[] vectorValue = receivedString.split(",");
                //System.out.println("vectorValue: " +);
                // holds only pertinent vector values., i.e. not sender or self
                LinkedList<String> vectorValueLean = new LinkedList<>();

                System.out.println();
                System.out.println("BUILDING LEAN FROM SENDER " + packet.getPort());
                for (int i = 0; i < vectorValue.length; i++){
                    System.out.print("Vector Value - " + vectorValue[i]);

                    // split and set destination and cost variables
                    String tempString = vectorValue[i];
                    String[] tempArray = tempString.split(":");
                    String destination = tempArray[0];
                    String cost = tempArray[1];


                    //System.out.println("Destination: " + destination);
                    //System.out.println("Cost " + cost);

                    // if destination isn't the sender or host, add to lean
                    if (destination.equals(senderPortString) || destination.equals(hostPortString)){
                        System.out.println(" Redundant, Not Added To Lean");
                    }else {
                        vectorValueLean.add(destination + ":" + cost);
                        System.out.println(" Added To Lean");
                    }

                    System.out.println("tempArray- " + Arrays.toString(tempArray));
                }

                // read and make node object for comparision
                Scanner scanner = new Scanner(new File(filename));
                String content; // holds file input for parsing
                boolean neighborLine = true; // marks first file line which is a list of neighbor nodes
                String routingTableEntry[]; // individual routing table entry, (destination, cost, next hop)

                System.out.println();
                while (scanner.hasNextLine()){
                    content = scanner.nextLine(); // reads individual line
                    System.out.println("Content: " + content);

                    // this is keep for writing back to file later
                    if (neighborLine){// first line is neighbor line
                        neighbors = content.split(",");
                        System.out.println(Arrays.toString(neighbors) + " Added As Neighbor");
                        neighborLine = false;
                    }else {// table entries which are analyzed and possibly updated with dvr algo
                        routingTableEntry = content.split(",");
                        routingTable.add(routingTableEntry);
                        System.out.println(Arrays.toString(routingTableEntry) + " Added As RTE");
                    }
                }
                routerNode = new RouterNode(neighbors, routingTable);
                System.out.println();
                System.out.println("Router Node Post Read");
                routerNode.printNode();

                /*
                while (scanner.hasNextLine()){
                content = scanner.nextLine(); // reads individual line
                System.out.println("Content: " + content);

                if (neighborLine){// first line is neighbor line
                    neighbors = content.split(",");
                    System.out.println(Arrays.toString(neighbors) + " Added As Neighbor");
                    neighborLine = false;
                }else {// remaining lines are individual table entries
                    routingTableEntry = content.split(",");
                    routingTable.add(routingTableEntry);
                    System.out.println(Arrays.toString(routingTableEntry) + " Added As RTE");
                }
            }
                 */

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
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! UPDATE EVENT BEGIN");
                        System.out.println("Sender: " + senderPortString);
                        System.out.println("Destination: " + tempDestination);
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
                    System.out.println();
                    System.out.println("WRITING BACK");
                    File file = new File(filename);
                    //FileWriter fileWriter = new FileWriter(file, false);
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, false));

                    System.out.println("Node pre write");
                    routerNode.printNode();

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