package routing;

import core.DTNHost;
import core.Message;

/**
 * Concrete implementation of AODV Route Request (RREQ) message.
 */
public class AODVRREQPacket extends Message {

    private static final String RREQ_PREFIX = "RREQ_";

    private int sourceAddr;
    private int sourceSeqNumber;
    private int broadcastId;
    private int destinationAddr;
    private int destinationSeqNumber;
    private int hopCount;

    public AODVRREQPacket(DTNHost source, DTNHost destination, String uniqueIdentifier,
                          int sourceAddr, int sourceSeqNumber, int broadcastId,
                          int destinationAddr, int destinationSeqNumber, int hopCount) {
        super(source, destination, RREQ_PREFIX + uniqueIdentifier, 0);
        this.sourceAddr = sourceAddr;
        this.sourceSeqNumber = sourceSeqNumber;
        this.broadcastId = broadcastId;
        this.destinationAddr = destinationAddr;
        this.destinationSeqNumber = destinationSeqNumber;
        this.hopCount = hopCount;
    }

    public int getSourceAddr() {
        return sourceAddr;
    }

    public int getSourceSeqNumber() {
        return sourceSeqNumber;
    }

    public int getBroadcastId() {
        return broadcastId;
    }

    public int getDestinationAddr() {
        return destinationAddr;
    }

    public int getDestinationSeqNumber() {
        return destinationSeqNumber;
    }

    public int getHopCount() {
        return hopCount;
    }

    @Override
    public AODVRREQPacket replicate() {
        AODVRREQPacket replicatedMessage = new AODVRREQPacket(
                this.getFrom(), this.getTo(), this.getId(),
                this.sourceAddr, this.sourceSeqNumber, this.broadcastId,
                this.destinationAddr, this.destinationSeqNumber, this.hopCount
        );
        replicatedMessage.copyFrom(this);
        return replicatedMessage;
    }
}
