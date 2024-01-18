package routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.Settings;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class AODVRouter extends ActiveRouter {
	
	private static final System.Logger LOGGER = System.getLogger(AODVRouter.class.getName());

	private static final Map<Integer, AODVRouter.RoutingTable> routingTableUpdates = new HashMap<>();


	
    private AODVRREQTable rreqTable;
    private RoutingTable routingTable;

    /**
     * Constructor. Creates a new message router based on the settings in the given
     * Settings object.
     * 
     * @param s The settings object
     */
    public AODVRouter(Settings s) {
        super(s);
        this.routingTable = new RoutingTable();
        this.rreqTable = new AODVRREQTable();
        // TODO: read & use epidemic router specific settings (if any)
    }

    /**
     * Copy constructor.
     * 
     * @param r The router prototype where setting values are copied from
     */
    protected AODVRouter(AODVRouter r) {
        super(r);
        this.routingTable = r.routingTable.clone(); // Assuming AODVRoutingTable has a clone method
        this.rreqTable = new AODVRREQTable();
        // TODO: copy epidemic settings here (if any)
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
        updateRoutingTable();

        // then try any/all message to any/all connection
        this.tryAllMessagesToAllConnections();
    }

    @Override
    public AODVRouter replicate() {
        return new AODVRouter(this);
    }
    
    // RREQ
    public class AODVRREQTable {
        private Map<String, AODVRREQPacket> rreqTable;

        public AODVRREQTable() {
            this.rreqTable = new HashMap<>();
        }

        public void addOrUpdateEntry(AODVRREQPacket rreqPacket) {
            rreqTable.put(rreqPacket.getUniqueIdentifier(), rreqPacket);
        }

        @Override
        public String toString() {
            return rreqTable.toString();
        }
    }

    public class AODVRREQPacket extends Message {
        private static final String RREQ_PREFIX = "RREQ_";

        private String uniqueIdentifier;
        private int sequenceNumber;
        private int hopCount;

        public AODVRREQPacket(DTNHost source, DTNHost destination, String uniqueIdentifier, int hopCount) {
            super(source, destination, RREQ_PREFIX + uniqueIdentifier, 0);
            this.uniqueIdentifier = uniqueIdentifier;
            this.sequenceNumber = Integer.parseInt(uniqueIdentifier.split("_")[2]);
            this.hopCount = hopCount;
        }


        public String getUniqueIdentifier() {
            return uniqueIdentifier;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }

        public int getHopCount() {
            return hopCount;
        }

        @Override
        public AODVRREQPacket clone() {
            return new AODVRREQPacket(this.getFrom(), this.getTo(), this.uniqueIdentifier, this.hopCount);
        }
    }
    
    // ROUTING TABLE
    public class RoutingTable {
        // Custom class to represent a routing table entry
        public static class RoutingEntry {
            private String destinationAddress;
            private String neighboringNodeAddress;
            private int sequenceNumber;
            private int hopCount;

            public RoutingEntry(String destinationAddress, String neighboringNodeAddress, int sequenceNumber, int hopCount) {
                this.destinationAddress = destinationAddress;
                this.neighboringNodeAddress = neighboringNodeAddress;
                this.sequenceNumber = sequenceNumber;
                this.hopCount = hopCount;
            }

            // Getter methods for the fields (add more if needed)

            public String getDestinationAddress() {
                return destinationAddress;
            }

            public String getNeighboringNodeAddress() {
                return neighboringNodeAddress;
            }

            public int getSequenceNumber() {
                return sequenceNumber;
            }

            public int getHopCount() {
                return hopCount;
            }

            @Override
            public String toString() {
                return String.format(
                        "Destination: %s, Neighboring Node: %s, Sequence Number: %d, Hop Count: %d",
                        destinationAddress, neighboringNodeAddress, sequenceNumber, hopCount);
            }
        }

        private Map<String, RoutingEntry> routingTable;

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
    /*
    private void updateRoutingTable() {
        // Iterate through incoming messages and update the routing table
        for (Message msg : getMessageCollection()) {
            if (msg) {
                AODVRREQPacket rreqPacket = (AODVRREQPacket) msg;

                // Extract relevant information from the RREQPacket
                String destinationAddress = rreqPacket.getTo().toString();
                String neighboringNodeAddress = rreqPacket.getFrom().toString();
                int sequenceNumber = rreqPacket.getSequenceNumber();
                int hopCount = rreqPacket.getHopCount();

                // Update or add the entry in the routing table
                RoutingTable.RoutingEntry routingEntry = new RoutingTable.RoutingEntry(destinationAddress, neighboringNodeAddress, sequenceNumber, hopCount);
                routingTable.addOrUpdateEntry(destinationAddress, routingEntry);

                // Add or update entry in the RREQ table
                rreqTable.addOrUpdateEntry(rreqPacket);

                // Log the update if needed
                System.out.println("Routing table updated: " + routingEntry);
                LOGGER.log(System.Logger.Level.INFO, "Routing table updated: " + routingEntry);
    
            }
        }
    }*/
    private void updateRoutingTable() {
        // Iterate through incoming messages and update the routing table
        for (Message msg : getMessageCollection()) {
            // Check if the message is relevant for updating the routing table
            String destinationAddress = msg.getTo().toString();
            String neighboringNodeAddress = msg.getFrom().toString();
            
            int routerAddres = this.getHost().getAddress();
            saveRoutingTableUpdateToList(routerAddres);
            
            // You need to define methods like getSequenceNumber() and getHopCount()
            // based on the actual structure of your Message class
            int sequenceNumber = getSequenceNumber(msg);
            int hopCount = getHopCount(msg);

            // Update or add the entry in the routing table
            RoutingTable.RoutingEntry routingEntry = new RoutingTable.RoutingEntry(destinationAddress, neighboringNodeAddress, sequenceNumber, hopCount);
            routingTable.addOrUpdateEntry(destinationAddress, routingEntry);

            // Log the update if needed
            System.out.println("Routing table updated: " + routingEntry);
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
    
}
