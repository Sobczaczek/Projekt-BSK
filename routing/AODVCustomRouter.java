package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AODVCustomRouter extends ActiveRouter {

    private AODVRREQTable rreqTable;
    private AODVRoutingTable routingTable;

    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     *
     * @param s The settings object.
     */
    public AODVCustomRouter(Settings s) {
        super(s);
        //this.rreqTable = new AODVRREQTable();
        //this.routingTable = new AODVRoutingTable();
    }

    protected AODVCustomRouter(AODVCustomRouter r) {
        super(r);
        this.rreqTable = new AODVRREQTable();
        this.routingTable = new AODVRoutingTable();
    }

    @Override
    protected void transferDone(Connection con) {
        Message message = con.getMessage();
        if (message instanceof AODVRRESPacket) {
            AODVRRESPacket rresPacket = (AODVRRESPacket) message;
            DTNHost source = con.getOtherNode(getHost());
            routingTable.updateRoutingEntry(source, rresPacket.getTo(), rresPacket.getSequenceNumber(), rresPacket.getHopCount());
        }
    }

    @Override
    protected int startTransfer(Message m, Connection con) {
        if (m instanceof AODVRREQPacket) {
            AODVRREQPacket rreqPacket = (AODVRREQPacket) m;
            handleRREQPacket(rreqPacket, con);
            return RCV_OK;
        }
        return super.startTransfer(m, con);
    }

    private void handleRREQPacket(AODVRREQPacket rreqPacket, Connection con) {
        DTNHost destination = rreqPacket.getTo();
        if (routingTable.hasRouteTo(destination)) {
            // Destination route is known, send RRES packet
            AODVRRESPacket rresPacket = new AODVRRESPacket(getHost(), destination, rreqPacket.getSequenceNumber(), 1);
            this.createNewMessage(rresPacket);
        } else {
            // Destination route is unknown, broadcast RREQ packet
            broadcastRREQPacket(rreqPacket);
        }
    }

    private void broadcastRREQPacket(AODVRREQPacket rreqPacket) {
        List<Connection> neighbors = getConnections();

        for (Connection neighborConnection : neighbors) {
            AODVRREQPacket clonedPacket = rreqPacket.clone();
            DTNHost neighborHost = neighborConnection.getOtherNode(getHost());

            this.sendMessage(clonedPacket.getUniqueIdentifier(), neighborHost);
        }
    }

    /**
     * Scenario 1. Destination route is NOT known.
     *
    private void handleUnknownRoute(Message m) {
        AODVRREQPacket rreqPacket = new AODVRREQPacket(m.getFrom(), m.getTo(), "1", m.getHopCount());
        rreqTable.addOrUpdateEntry(rreqPacket);
        broadcastRREQPacket(rreqPacket);
    }*/

    /**
     * Scenario 2. Destination route is known.
     *
    private void handleKnownRoute(Message m) {
        // AODV specific logic for handling known route
        // Using the routing table to forward the message along the known route
    }*/

    // ADDITIONAL CLASSES
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

    public class AODVRRESPacket extends Message {
        private static final String RRES_PREFIX = "RRES_";

        private int sequenceNumber;
        private int hopCount;

        public AODVRRESPacket(DTNHost source, DTNHost destination, int sequenceNumber, int hopCount) {
            super(source, destination, RRES_PREFIX + sequenceNumber, 0);
            this.sequenceNumber = sequenceNumber;
            this.hopCount = hopCount;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }

        public int getHopCount() {
            return hopCount;
        }

        @Override
        public AODVRRESPacket clone() {
            return new AODVRRESPacket(this.getFrom(), this.getTo(), this.sequenceNumber, this.hopCount);
        }
    }

    public class AODVRoutingTable {
        private Map<String, AODVRoutingEntry> routingTable;

        public AODVRoutingTable() {
            this.routingTable = new HashMap<>();
        }

        public void updateRoutingEntry(DTNHost source, DTNHost destination, int sequenceNumber, int hopCount) {
            String key = getRoutingEntryKey(destination);
            AODVRoutingEntry entry = routingTable.get(key);

            if (entry == null || sequenceNumber > entry.getSequenceNumber()) {
                entry = new AODVRoutingEntry(source, sequenceNumber, hopCount);
                routingTable.put(key, entry);
            }
        }

        public boolean hasRouteTo(DTNHost destination) {
            return routingTable.containsKey(getRoutingEntryKey(destination));
        }

        public AODVRoutingEntry getRoutingEntry(DTNHost destination) {
            return routingTable.get(getRoutingEntryKey(destination));
        }

        private String getRoutingEntryKey(DTNHost host) {
            return host.toString();
        }

        @Override
        public String toString() {
            return routingTable.toString();
        }
    }

    public class AODVRoutingEntry {
        private DTNHost source;
        private int sequenceNumber;
        private int hopCount;

        public AODVRoutingEntry(DTNHost source, int sequenceNumber, int hopCount) {
            this.source = source;
            this.sequenceNumber = sequenceNumber;
            this.hopCount = hopCount;
        }

        public DTNHost getSource() {
            return source;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }

        public int getHopCount() {
            return hopCount;
        }

        @Override
        public String toString() {
            return "AODVRoutingEntry{" +
                    "source=" + source +
                    ", sequenceNumber=" + sequenceNumber +
                    ", hopCount=" + hopCount +
                    '}';
        }
    }
}
