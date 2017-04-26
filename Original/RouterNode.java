import java.util.Arrays;
import java.util.LinkedList;

public class RouterNode {

    private String[] neighbors;
    private LinkedList<String[]> routingTable;

    public RouterNode(String[] neighbors, LinkedList<String[]> routingTable) {
        this.neighbors = neighbors;
        this.routingTable = routingTable;
    }

    public RouterNode() {
    }

    public String[] getNeighbors() {
        return neighbors;
    }

    public LinkedList<String[]> getRoutingTable() {
        return routingTable;
    }

    public String getDistanceVectorValues(LinkedList<String[]> routingTable){

        StringBuilder stringBuilder = new StringBuilder();

        // if-else prevents ',' character after last entry
        for (int i = 0; i < routingTable.size(); i++){
            String[] temp = routingTable.get(i);
            if (i != routingTable.size() - 1){
                stringBuilder.append(temp[0]+":"+temp[1]+",");
            }else {
                stringBuilder.append(temp[0]+":"+temp[1]);
            }
        }

        return stringBuilder.toString();
    }

    public String getCost(String destination){

        String returnCost = null;

        for (int i = 0; i < routingTable.size(); i++){
            String[] temp = routingTable.get(i);
            String destinationTemp = temp[0];
            String costTemp = temp[1];

            if (destination.equals(destinationTemp)){
                returnCost = costTemp;
                break;
            }
        }

        return returnCost;
    }

    public void updateDistanceVectorEntry(String[] newRoutingTableEntry){

        String newDestination = newRoutingTableEntry[0];
        //String newCost = newRoutingTableEntry[1];
        //String newNextHop = newRoutingTableEntry[2];

        for (int i =0; i < routingTable.size(); i++){
            String[] temp = routingTable.get(i);
            String tempDestination = temp[0];

            if (newDestination.equals(tempDestination)){
                routingTable.set(i, newRoutingTableEntry);
                break;
            }
        }
    }

    @Override
    public String toString() {

        System.out.print("Direct Neighbors: ");
        for (int i = 0; i < neighbors.length; i++){
            System.out.print(neighbors[i] + ", ");
        }

        System.out.println("\n\nRouting Table ");
        System.out.println("Destination" + "\t" + "Cost" + "\t\t" + "Next Hop");
        for (int i = 0; i < routingTable.size(); i++){
            String[] temp = routingTable.get(i);
            System.out.print(temp[0] + "\t\t" + temp[1] + "\t\t" + temp[2] + "\n");
        }
        return null;
    }
}
