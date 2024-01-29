package routing;

import core.DTNHost;
import core.Message;

public abstract class RREQPacket extends Message {

    // Add specific fields/methods for RREQPacket if needed
	private String dupa;

    public RREQPacket(DTNHost from, DTNHost to, String id, int size, String dupa) {
        super(from, to, id, size);
        this.dupa = dupa;
    }
    
    public String getDupa() {
        return dupa;
    }
}
