package routing;

/**
 * Concrete implementation of AODV Route Reply (RREP) message.
 */
public class AODVRREPPacket {

    private int sourceAddr;
    private int destinationAddr;
    private int destinationSeqNumber;
    private int hopCount;
    private int lifetime;

    public AODVRREPPacket(int sourceAddr, int destinationAddr, int destinationSeqNumber, int hopCount, int lifetime) {
        this.destinationAddr = destinationAddr;
        this.destinationSeqNumber = destinationSeqNumber;
        this.hopCount = hopCount;
        this.lifetime = lifetime;
    }

    public int getSourceAddr() {
        return sourceAddr;
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

    public int getLifetime() {
        return lifetime;
    }
    
    /*
    public AODVRREPPacket replicate() {
        AODVRREPPacket replicatedMessage = new AODVRREPPacket(
                this.sourceAddr, this.destinationAddr, this.destinationSeqNumber,
                this.hopCount, this.lifetime
        );
        replicatedMessage.copyFrom(this);
        return replicatedMessage;
    }*/
    

}
