package routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;


public class AODVRouter extends ActiveRouter {
	
	private static final Map<Integer, AODVRouter.RoutingTable> routingTableUpdates = new HashMap<>();
    
	private RoutingTable routingTable;

    public AODVRouter(Settings s) {
        super(s);
        this.routingTable = new RoutingTable();
    }

    protected AODVRouter(AODVRouter r) {
        super(r);
        this.routingTable = r.routingTable.clone();
    }
    
    @Override
    public AODVRouter replicate() {
        return new AODVRouter(this);
    }

    @Override
    public void update() {
        super.update();
        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }

        // Try first the messages that can be delivered to the final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }

        // Update the routing table based on incoming messages or any other criteria
        // updateRoutingTable();
        

        // then try any/all message to any/all connection
        this.tryAllMessagesToAllConnections();
    }
    
    /**
     * Node A connect to Node B.
     */
    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);
        /*
        if (con.isUp()) {
            DTNHost peer = con.getOtherNode(getHost());
            List<Message> newMessages = new ArrayList<Message>();
            
            for (Message m : peer.getMessageCollection()) {
            	if (!this.hasMessage(m.getId())) {
            		newMessages.add(m);
            	}
            }
            
            for (Message m : newMessages) {
            	if (con.startTransfer(peer, m) == RCV_OK) {
            		con.finalizeTransfer(); 
            	}
            }
            
            
            
            
            
            MessageRouter peerRouter = peer.getRouter();

            // Simulate the exchange of RouteRequest packets
            AODVRREPPacket rrepPacket = processRouteRequest(peer);

            // Simulate the reception of RouteReply packet
            if (rrepPacket != null) {
                // Assuming you have a connection to the peer
                Connection peerConnection = getConToHost(peer);

            }
        }*/
        if (con.isUp()) {
            DTNHost peer = con.getOtherNode(getHost());
            RREQPacket rreqPacket = new RREQPacket(this.getHost(), peer, "someId", 10, "dupa") {};
            if (peer != null) {
                // Assuming you have an instance of RREQPacket named rreqPacket
                broadcastRREQPacket(rreqPacket);
            }
        }
    }
    
    private AODVRREPPacket processRouteRequest(DTNHost destination) {
        int destinationAddr = destination.getAddress();
        AODVRREPPacket rrep = new AODVRREPPacket(getHost().getAddress(), destinationAddr, 0, 0, 120);

        return rrep;
    }
    

 // Add this method to your AODVRouter class
    private Connection getConToHost(DTNHost destination) {
        // Assuming you have a list of connections, iterate through them to find the one to the destination
        for (Connection con : getConnections()) {
            if (con.getOtherNode(getHost()) == destination) {
                return con;
            }
        }
        return null; // Connection not found
    }



        
    /**
     * Routing Table class.
     */
    public class RoutingTable {
    	
    	/**
    	 * Routing table entry.
    	 */
        public static class RoutingEntry {
        	
            private int destinationAddr;
            private int nextHopAddr;
            private int sequenceNr;
            private int hopCount;
            
            /**
             * Constructor. Routing Entry.
             * @param destinationAddr
             * @param nextHopAddr
             * @param sequenceNr
             * @param hopCount
             */
            public RoutingEntry(int destinationAddr, int nextHopAddr, int sequenceNr, int hopCount) 
            {
                this.destinationAddr = destinationAddr;
                this.nextHopAddr = nextHopAddr;
                this.sequenceNr = sequenceNr;
                this.hopCount = hopCount;
            }

            public int getDestinationAddr() {
                return destinationAddr;
            }

            public int getNextHopAddr() {
                return nextHopAddr;
            }

            public int getSequenceNr() {
                return sequenceNr;
            }

            public int getHopCount() {
                return hopCount;
            }

            @Override
            public String toString() {
                return String.format(
                        "Destination: %d, Next Node: %d, Sequence Number: %d, Hop Count: %d",
                        destinationAddr, nextHopAddr, sequenceNr, hopCount);
            }
        }

        private Map<String, RoutingEntry> routingTable;

        /**
         * Constructor.
         */
        public RoutingTable() {
            this.routingTable = new HashMap<>();
        }

        public void addOrUpdateEntry(String key, RoutingEntry value) {
            routingTable.put(key, value);
        }

        public RoutingTable clone() {
            
        	RoutingTable clonedTable = new RoutingTable();
            for (Map.Entry<String, RoutingEntry> entry : this.routingTable.entrySet()) {
                clonedTable.addOrUpdateEntry(entry.getKey(), entry.getValue());
            }
            return clonedTable;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Routing Table:\n");
            for (RoutingEntry entry : routingTable.values()) {
                builder.append(entry.toString()).append("\n");
            }
            return builder.toString();
        }
    }

    private void updateRoutingTable() {
        // Iterate through incoming messages and update the routing table
        for (Message msg : getMessageCollection()) {
            // Check if the message is relevant for updating the routing table
            // String destinationAddress = msg.getTo().toString();
            // String neighboringNodeAddress = msg.getFrom().toString();
            
            int routerAddres = this.getHost().getAddress();
            saveRoutingTableUpdateToList(routerAddres);
            
            // You need to define methods like getSequenceNumber() and getHopCount()
            // based on the actual structure of your Message class
            int sequenceNumber = getSequenceNumber(msg);
            int hopCount = getHopCount(msg);

            // Update or add the entry in the routing table
            // RoutingTable.RoutingEntry routingEntry = new RoutingTable.RoutingEntry(destinationAddress, neighboringNodeAddress, sequenceNumber, hopCount);
            // routingTable.addOrUpdateEntry(destinationAddress, routingEntry);

            // Log the update if needed
            // System.out.println("Routing table updated: " + routingEntry);
        }
    }

    // Example methods that need to be adapted based on your actual Message class
    private boolean isRelevantMessage(Message msg) {
        // Add your logic to determine if the message is relevant for updating the routing table
        // For example, check if it is an AODVRREQPacket or any other relevant criteria
        return msg instanceof AODVRREQPacket;
    }

    private int getSequenceNumber(Message msg) {
        // Replace this with the actual method to get the sequence number from the Message
        return 0; // Example value, replace it with your logic
    }

    private int getHopCount(Message msg) {
        // Replace this with the actual method to get the hop count from the Message
        return 0; // Example value, replace it with your logic
    }
    
    private void saveRoutingTableUpdateToList(int routerAddress) {
        // Add the current routing table to the map with the router ID as the key
        routingTableUpdates.put(routerAddress, routingTable.clone());
    }

    public static Map<Integer, AODVRouter.RoutingTable> getRoutingTableUpdates() {
        return routingTableUpdates;
    }
    
    
    
    
    
    /*
    AODVRREPPacket processRouteRequest(DTNHost destination) {
    	int destinationAddr = destination.getAddress();
    	AODVRREPPacket rrep = new AODVRREPPacket(getHost().getAddress(), destinationAddr, 0, 0, 120);
    	
    	return rrep;
    }*/
    private AODVRREPPacket processRouteRequestAndGetReply(int destinationAddr) {
        // Process the RouteRequest and generate a RouteReply packet
        // You need to implement the logic for processing the RouteRequest and generating the reply
        // For simplicity, I'll create a dummy AODVRREPPacket
        return new AODVRREPPacket(getHost().getAddress(), destinationAddr, 0, 0, 120);
    }
    
    public void broadcastRREQPacket(RREQPacket rreqPacket) {
        for (Connection connection : getConnections()) {
            DTNHost peer = connection.getOtherNode(getHost());
            if (peer != null) {
                // Send the RREQPacket to the connected router
                connection.startTransfer(peer, rreqPacket);
                connection.finalizeTransfer();
            }
        }
    }
    
}
