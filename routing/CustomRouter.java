/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.CustomRouter.RoutingTable.RoutingEntry;
import util.Tuple;


/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class CustomRouter extends ActiveRouter {
	
	private RoutingTable routingTable;
	
	private List<Message> messageWaitList;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public CustomRouter(Settings s) {
		super(s);
		this.routingTable = new RoutingTable();
		this.messageWaitList = new ArrayList<Message>();
		//TODO: read&use epidemic router specific settings (if any)
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected CustomRouter(CustomRouter r) {
		super(r);
		this.routingTable = r.routingTable.clone();
		this.messageWaitList = r.messageWaitList;
	}
	
	/**
	 * Routing Table.
	 */
	public class RoutingTable{
		/**
		 * Routing Table Entry.
		 */
		public static class RoutingEntry{
			private DTNHost nextHop;
			private int hopCount;
			
			/**
			 * Constructor.
			 * @param destination
			 * @param nextHop
			 */
			public RoutingEntry(DTNHost nextHop, int hopCount) {
				this.nextHop = nextHop;
				this.hopCount = hopCount;
			}
			
			public RoutingEntry(int hopCount) {
				this.hopCount = hopCount;
			}
						
			public DTNHost getNextHop() {
				return nextHop;
			}
			
			public int getHopCount() {
				return hopCount;
			}
			
            @Override
            public String toString() {
                return String.format(
                        "NextHop: %s, HopCount: %d",
                        nextHop.toString(), hopCount);
            }
		}
		
		private Map<DTNHost, RoutingEntry> routingTable;
		
        /**
         * Constructor.
         */
        public RoutingTable() {
            this.routingTable = new HashMap<>();
        }
        
        public void addOrUpdateEntry(DTNHost destination, RoutingEntry value) {
            routingTable.put(destination, value);
        }
        
        /**
         * 
         * @param destination
         * @return
         */
        public boolean routingTableEntryPresentBool(DTNHost destination) {
        	for (Map.Entry<DTNHost, RoutingEntry> entry : routingTable.entrySet()) {
        			
        		DTNHost entryKey = entry.getKey();
        		
        		if (entryKey.equals(destination)) {
        			return true;
        		}
        	}
        	
        	return false;
        }
        
        public boolean routingTableEntryPresentHopCountLower(DTNHost destination, int hopcount) {
        	for (Map.Entry<DTNHost, RoutingEntry> entry : routingTable.entrySet()) {
        			
        		DTNHost entryKey = entry.getKey();
        		
        		if (entryKey.equals(destination)) {
        			if (hopcount >= entry.getValue().getHopCount()) {
            			return true;
        			}
            		else {
            			return false;
            		}
        		}
        	}
        	
        	return false;
        }
        
       
        
        public RoutingEntry routingTableEntryPresent(DTNHost messageDestination) {
        	for (Map.Entry<DTNHost, RoutingEntry> entry : routingTable.entrySet()) {
    			
        		DTNHost entryKey = entry.getKey();
        		RoutingEntry entryValue = entry.getValue();
        		
        		if (entryKey.equals(messageDestination)) {
        			return entryValue;
        		}
        	}
        	
        	return null;
        }
        
        public RoutingTable clone() {
            
        	RoutingTable clonedTable = new RoutingTable();
            for (Map.Entry<DTNHost, RoutingEntry> entry : this.routingTable.entrySet()) {
                clonedTable.addOrUpdateEntry(entry.getKey(), entry.getValue());
            }
            return clonedTable;
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Routing Table:\n");

            for (Map.Entry<DTNHost, RoutingEntry> entry : routingTable.entrySet()) {
                DTNHost key = entry.getKey();
                RoutingEntry value = entry.getValue();

                builder.append("[").append(key.toString()).append(": ").append(value.toString()).append("]\n");
            }

            return builder.toString();
        }
		
	}
	
	protected Connection exchangeDeliverableMessages() {
		List<Connection> connections = getConnections();

		if (connections.size() == 0) {
			return null;
		}
		
		@SuppressWarnings(value = "unchecked")
		Tuple<Message, Connection> t =
			tryMessagesForConnected(sortByQueueMode(getMessagesForConnected()));

		if (t != null) {
			return t.getValue(); // started transfer
		}
		
		// didn't start transfer to any node -> ask messages from connected
		for (Connection con : connections) {
			if (con.getOtherNode(getHost()).requestDeliverableMessages(con)) {
				return con;
			}
		}
		
		return null;
	}
	
	@Override
	protected Connection tryAllMessagesToAllConnections() {
		List<Connection> connections = getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}

		List<Message> messages = 
			new ArrayList<Message>(this.getMessageCollection());
		this.sortByQueueMode(messages);

		return tryMessagesToConnections(messages, connections);
	}
	
	protected Connection broadcastRREQPacketMessage(Message message) {
		List<Connection> connections = getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}

		List<Message> messages = new ArrayList<Message>();
		messages.add(message);
		
		//edit
		for (Connection con : connections) {
			startTransfer(message, con);
		}
		
		
		return tryMessagesToConnections(messages, connections);
	}
	
	protected boolean sendMessageToConnection(Message message) {
		List<Connection> connections = getConnections();
		if (connections.size() == 0) {
			return false;
		}
		for (Connection con : connections) {
			int connection_addr = con.getOtherNode(getHost()).getAddress();
			int to_addr = message.getTo().getAddress();
			
			if (connection_addr == to_addr) {
				int retVal = startTransfer(message, con);
				if (retVal == RCV_OK) {
					return true;	// accepted a message, don't try others
				}
			}
		}
		return false;
	}
	
	/**
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}
		
		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}
		if (getHost().toString().equals("b13")) {
			System.out.println();
		}
		
		// GET ALL HOST MESSAGES
		System.out.println(String.format("\nCurrent Host: %s", getHost().toString()));
		
		Collection<Message> newMessagesCollection = getHost().getMessageCollection();
		// Using type-casting to convert from Collection<Message> to List<Message>
	    List<Message> newMessages = new ArrayList<>(newMessagesCollection);
	    
	    //for (Message m : newMessages ) {
	    //	System.out.println(String.format("Message: %s", m.toString()));
	    //}
	    
	    for (Message m : newMessages) {
	    	
	    	DTNHost messageDestination = m.getTo();
	    	DTNHost messageSource = m.getFrom();
	    	
	    	// check if RREQ Message
	    	if (m.getId().contains("RREQ")) {
	    		
	    		if(m.getTo() == getHost()) {
	    			// prepare RREP Packet
	    			System.out.println("I've reached destination! Send RREP");
	    		}
	    		else {
		    		System.out.println(String.format("RREQ Packet detected: %s", m.getId()));
		    		
		    		List<DTNHost> hops = m.getHops();
		    		
		    		if (hops.size() >= 2) {
		    			
		    			DTNHost nextHop =  hops.get(hops.size() - 2);
		    			int hopCount = hops.size();
			    		
			    		if (nextHop != getHost()){
				    		
			    			RoutingEntry newEntry = new RoutingEntry(nextHop, hopCount);
				    		this.routingTable.addOrUpdateEntry(messageSource, newEntry);
				    		
				    		System.out.println(String.format("%s: New Routing Entry: ", getHost().toString()));
			    		}
		    		}
		    		//broadcast it further
		    		Connection sentTo = broadcastRREQPacketMessage(m);
		    		System.out.println(String.format("%s: Broadcasting to all connections!", m.toString()));
		    		if(sentTo != null) {
		    			System.out.println(String.format("Sent to: %s", sentTo.toString()));
		    		}
	    		}
	    	}
	    	// process message
	    	else {
		    	// extract Destination
		    	System.out.println(String.format("Message: %s From: %s: To: %s", m.toString(), messageSource, messageDestination));
		    	
		    	// check if routing table entry with that destination is present in Host Routing Table
		    	if (this.routingTable.routingTableEntryPresentBool(messageDestination)) {
		    		System.out.println(String.format("%s: Routing Entry found", m.toString()));
		    		//sendMessageToConnection(m);
		    		//#TODO send message to host from routing table
		    	}
		    	else {
		    		System.out.println(String.format("%s: Routing Entry not found", m.toString()));
		    		
		    		if (this.messageWaitList != null || !(this.messageWaitList.isEmpty())) {
		    			if (this.messageWaitList.contains(m)) {
		    				System.out.println(String.format("Message: %s wait!", m.toString()));
		    			}
		    			else {
				    		Message rreqPacketMessage = new Message(messageSource, messageDestination, String.format("%s_RREQ", m.getId()) , 68000);
				    		getHost().createNewMessage(rreqPacketMessage);
				    		// broadcast this new RREQ Packet
				    		Connection sentTo = broadcastRREQPacketMessage(rreqPacketMessage);
				    		System.out.println(String.format("%s: Broadcasting to all connections!", rreqPacketMessage.toString()));
				    		if(sentTo != null) {
				    			System.out.println(String.format("Sent to: %s", sentTo.toString()));
				    		}
				    		// add message to waitList
				    		this.messageWaitList.add(m);		    		
		    			}
		    		}
		    					
		    	}
	    		
	    	}
	    	

	    }
	    System.out.println(String.format("%s: %s", getHost().toString(), this.routingTable.toString()));
	    
	    
	    
		// then try any/all message to any/all connection
		// this.tryAllMessagesToAllConnections();
	}*/
	
	


	@Override
	public CustomRouter replicate() {
		return new CustomRouter(this);
	}
	
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return;
		}
		
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		System.out.println(String.format("%s: %s", getHost().toString(), this.routingTable.toString()));
		
		//this.tryAllMessagesToAllConnections();
	}
	
	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		/**
		if (con.isUp()) {
			DTNHost peer = con.getOtherNode(getHost());
			//System.out.println(String.format("Host: %s Con: %s.", getHost().toString(),peer.toString()));
			
			List<Message> newMessages = new ArrayList<Message>();
			
			for (Message m : getHost().getMessageCollection()) {
				if (!this.hasMessage(m.getId())) {
					newMessages.add(m);
					System.out.println(String.format("Message: %s", m.getId()));
				}
			}
			for (Message m : newMessages) {
				if (con.startTransfer(peer, m) == RCV_OK) {
					con.finalizeTransfer();
					DTNHost messageTo = m.getTo();
					DTNHost messageFrom = m.getFrom();
					
					System.out.println(String.format("Host: %s Peer: %s MID: %s Transfered!", getHost(), con.getOtherNode(getHost()) ,m.getId()));
					System.out.println(String.format("MID: %s From: %s To: %s", m.getId(), messageFrom, messageTo));
					System.out.println();
				}
			}
		}*/
		if (con.isUp()) {
			System.out.println(); //clear line
			System.out.println(String.format("~Connection is up!"));
			
			// get this host
			DTNHost host = getHost();
			System.out.println(String.format("Current host: %s!", host.toString()));
			
			// get connection peer
			DTNHost peer = con.getOtherNode(getHost());
			System.out.println(String.format("Connection to: %s!", peer.toString()));
			
			// get peer messages
			List<Message> newMessages = new ArrayList<Message>();
			
			for (Message m : peer.getMessageCollection()) {
				if (!this.hasMessage(m.getId())) {
					newMessages.add(m);
					System.out.println(String.format("Message: %s", m.getId()));
				}
			}
			// iterate through new messages
			for (Message m : newMessages) {
				// message
				DTNHost messageFrom = m.getFrom();
				DTNHost messageTo = m.getTo();
				
				// if host is message destination
				if (messageTo == host) {
					// check if rrep
					if (m.getId().contains("RREP")) {
						// #TODO now we know route?
						System.out.println();
						// accept it
						if (con.startTransfer(peer, m) == RCV_OK) {
							con.finalizeTransfer();
							System.out.println(String.format("MID: %s received!", m.getId()));
						}
						// remove original message from waitlist
						this.messageWaitList.remove(m);
					}
					// check if rreq
					else if (m.getId().contains("RREQ")) {
						// create rrep
			    		Message rrepPacketMessage = new Message(messageTo, messageFrom, String.format("%s_RREP", m.getId()) , 68000);
			    		rrepPacketMessage.setTtl(120);
			    		getHost().createNewMessage(rrepPacketMessage);
			    		// accept it
						if (con.startTransfer(peer, m) == RCV_OK) {
							con.finalizeTransfer();
							System.out.println(String.format("MID: %s received!", m.getId()));
						}
					}
					else {
						// accept normal message - delivered
						if (con.startTransfer(peer, m) == RCV_OK) {
							con.finalizeTransfer();
							System.out.println(String.format("MID: %s received!", m.getId()));
						}	
					}

				}
				else if(messageTo != host){
					if (this.routingTable.routingTableEntryPresentBool(messageTo)) {
						System.out.println(String.format("Routing Entry found"));
						// accept message because host knows path 
						if (con.startTransfer(peer, m) == RCV_OK) {
							con.finalizeTransfer();
							System.out.println(String.format("MID: %s received!", m.getId()));
						}
						// update routing table?
						if (!this.routingTable.routingTableEntryPresentHopCountLower(messageTo, m.getHops().size())){
				    		List<DTNHost> hops = m.getHops();
				    		
				    		if (hops.size() >= 2) {
				    			
				    			DTNHost nextHop =  hops.get(hops.size() - 2);
				    			int hopCount = hops.size();
					    		
					    		if (nextHop != getHost()){
						    		
					    			RoutingEntry newEntry = new RoutingEntry(nextHop, hopCount);
						    		this.routingTable.addOrUpdateEntry(messageFrom, newEntry);
						    		
						    		System.out.println(String.format("%s: Routing Entry updated: ", getHost().toString()));
					    		}
				    		}
						}
						//#TODO send message to route
						System.out.println();
					}
					else {
						System.out.println(String.format("Routing Entry not found"));
						// check if message is RREQ packet
						if (m.getId().contains("RREQ")) {
							if (con.startTransfer(peer, m) == RCV_OK) {
								con.finalizeTransfer();
								System.out.println(String.format("MID: %s received!", m.getId()));
								// create routing table entry
					    		System.out.println(String.format("RREQ Packet detected: %s", m.getId()));
					    		
					    		List<DTNHost> hops = m.getHops();
					    		
					    		if (hops.size() >= 2) {
					    			
					    			DTNHost nextHop =  hops.get(hops.size() - 2);
					    			int hopCount = hops.size();
						    		
						    		if (nextHop != getHost()){
							    		
						    			RoutingEntry newEntry = new RoutingEntry(nextHop, hopCount);
							    		this.routingTable.addOrUpdateEntry(messageFrom, newEntry);
							    		
							    		System.out.println(String.format("%s: New Routing Entry: ", getHost().toString()));
						    		}
					    		}
							}
						}
						else {
							// create new RREQ
				    		if (this.messageWaitList != null || !(this.messageWaitList.isEmpty())) {
				    			if (this.messageWaitList.contains(m)) {
				    				System.out.println(String.format("Message: %s wait!", m.toString()));
				    			}
				    			else {
						    		Message rreqPacketMessage = new Message(messageFrom, messageTo, String.format("%s_RREQ", m.getId()) , 68000);
						    		rreqPacketMessage.setTtl(120);
						    		getHost().createNewMessage(rreqPacketMessage);
						    		// broadcast this new RREQ Packet
						    		// Connection sentTo = broadcastRREQPacketMessage(rreqPacketMessage);
						    		//System.out.println(String.format("%s: Broadcasting to all connections!", rreqPacketMessage.toString()));
						    		//if(sentTo != null) {
						    		//	System.out.println(String.format("Sent to: %s", sentTo.toString()));
						    		//}
						    		// add message to waitList
						    		this.messageWaitList.add(m);		    		
				    			}
				    		}
						}
					}
					
				}
				else {
					//
					System.out.println(String.format("what"));
				}
			}
			
			// get Host messages
			// List<Message> hostMessages = new ArrayList<Message>();
			
			/*
			System.out.println(String.format("%s messages:", host.toString()));
			for (Message m : host.getMessageCollection()) {
				System.out.println(String.format("MID: %s", m.getId()));
				// get message destination
				DTNHost messageTo = m.getTo();
				
				// check routing table entry for that destination
				if (this.routingTable.routingTableEntryPresentBool(messageTo)) {
					//#TODO send message to route
				}
				else {
					System.out.println(String.format("%Routing Entry not found", m.toString()));
		    		// create RREQ Packet
					Message rreqPacketMessage = new Message(host, messageTo, String.format("%s_RREQ", m.getId()) , 68000);
		    		getHost().createNewMessage(rreqPacketMessage);
				}
			}*/
		}
		
	}

}