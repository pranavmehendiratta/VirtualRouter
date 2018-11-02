package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import java.util.*;
import java.util.concurrent.*;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.*;
import java.nio.ByteBuffer;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
    /** Routing table for the router */
    private RouteTable routeTable;

    /** ARP cache for the router */
    private ArpCache arpCache;

    /** Queue for packets whose ARP is unavailable */
    private ArpQO arpObj;

    /**
     * Creates a router for a specific host.
     * @param host hostname for the router
     */
    public Router(String host, DumpFile logfile)
    {
	super(host,logfile);
	this.routeTable = new RouteTable();
	this.arpCache = new ArpCache();
	this.arpObj = new ArpQO();
    }

    /**
     * @return routing table for the router
     */
    public RouteTable getRouteTable()
    { return this.routeTable; }

    /**
     * Load a new routing table from a file.
     * @param routeTableFile the name of the file containing the routing table
     */
    public void loadRouteTable(String routeTableFile)
    {
	if (!routeTable.load(routeTableFile, this))
	{
	    System.err.println("Error setting up routing table from file "
		    + routeTableFile);
	    System.exit(1);
	}

	System.out.println("Loaded static route table");
	System.out.println("-------------------------------------------------");
	System.out.print(this.routeTable.toString());
	System.out.println("-------------------------------------------------");
    }

    /**
     * Load a new ARP cache from a file.
     * @param arpCacheFile the name of the file containing the ARP cache
     */
    public void loadArpCache(String arpCacheFile)
    {
	if (!arpCache.load(arpCacheFile))
	{
	    System.err.println("Error setting up ARP cache from file "
		    + arpCacheFile);
	    System.exit(1);
	}

	System.out.println("Loaded static ARP cache");
	System.out.println("----------------------------------");
	System.out.print(this.arpCache.toString());
	System.out.println("----------------------------------");
    }

    /**
     * Handle an Ethernet packet received on a specific interface.
     * @param etherPacket the Ethernet packet that was received
     * @param inIface the interface on which the packet was received
     */
    public void handlePacket(Ethernet etherPacket, Iface inIface)
    {
	System.out.println("*** -> Received packet: " +
		etherPacket.toString().replace("\n", "\n\t"));

	/********************************************************************/
	/* TODO: Handle packets                                             */

	if (etherPacket.getEtherType() == Ethernet.TYPE_IPv4) {
	    System.out.println("Packet type is IPv4");
	} else if (etherPacket.getEtherType() == Ethernet.TYPE_ARP) {
	    System.out.println("Packet type is ARP");	
	}

	switch(etherPacket.getEtherType())
	{
	    case Ethernet.TYPE_IPv4:
		this.handleIpPacket(etherPacket, inIface);
		break;
	    case Ethernet.TYPE_ARP:
		short opCode = ((ARP)etherPacket.getPayload()).getOpCode();
		this.handleARPPacket(etherPacket, inIface, opCode);
		break;
	}

	/********************************************************************/
    }

    private void handleARPPacket(Ethernet etherPacket, Iface inIface, short opCode) {
	System.out.println("Inside handleARPPacket");
	
	// ARP packet
	ARP arpPacket = (ARP)etherPacket.getPayload();
	
	// Send a ARP reply if the arp packet contains the opcode of an ARP request
	if (opCode == ARP.OP_REQUEST) {

	    // send arp reply if interface ip = packet ip
	    int targetIP = ByteBuffer.wrap(arpPacket.getTargetProtocolAddress()).getInt();

	    System.out.println("ARP.OP_REQUEST");

	    System.out.println("Target ip: " + targetIP);
	    System.out.println("interface ip: " + inIface.getIpAddress());

	    
	    if (targetIP == inIface.getIpAddress()) {
	   
		System.out.println("TargetIP = Interface IP");

		// create ethernet packet
		Ethernet ether = new Ethernet();
		ether.setEtherType(Ethernet.TYPE_ARP);
		
		// source mac of the packet - interface on which we received initially
		ether.setSourceMACAddress(inIface.getMacAddress().toBytes());

		// set destination mac
		ether.setDestinationMACAddress(etherPacket.getSourceMACAddress());

		// Create ARP packet
		ARP arp = new ARP();

		arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
		arp.setProtocolType(ARP.PROTO_TYPE_IP);
		arp.setHardwareAddressLength((byte)Ethernet.DATALAYER_ADDRESS_LENGTH);
		arp.setProtocolAddressLength((byte)4);
		arp.setOpCode(ARP.OP_REPLY);
		arp.setSenderHardwareAddress(inIface.getMacAddress().toBytes());
		arp.setSenderProtocolAddress(inIface.getIpAddress());
		arp.setTargetHardwareAddress(arpPacket.getSenderHardwareAddress());
		arp.setTargetProtocolAddress(arpPacket.getSenderProtocolAddress());
		
		ether.setPayload(arp);
		
		if (arpObj.queue.containsKey(targetIP)) {
		    ArpQOData entry = arpObj.queue.get(targetIP);
		    
		    // Prevent more Arp requests from being sent
		    entry.request = true;
		    
		    System.out.println("Reply found for targetIP: " + targetIP);
		    System.out.println("Current hashmap state is ----> ");
		    arpObj.print();

		    // Add to Arp cache 

		    // TODO: Make sure to add destination MAC address when ARP request is
		    // fulfilled

		    // Send all the packets

		    // Remove the key from the hashmap
		}

		this.sendPacket(ether, inIface);

	    }
	} else if (opCode == ARP.OP_REPLY) {
	    // send arp reply if interface ip = packet ip
	    int targetIP = ByteBuffer.wrap(arpPacket.getTargetProtocolAddress()).getInt();
	    
	    // create ethernet packet
	    Ethernet ether = new Ethernet();
	    ether.setEtherType(Ethernet.TYPE_ARP);

	    // source mac of the packet - interface on which we received initially
	    ether.setSourceMACAddress(inIface.getMacAddress().toBytes());

	    // set destination mac
	    ether.setDestinationMACAddress("FF:FF:FF:FF:FF:FF");

	    // Create ARP packet
	    ARP arp = new ARP();
	    arp.setOpCode(ARP.OP_REQUEST);
	    byte [] hardAddr = new byte[6];
	    Arrays.fill(hardAddr, (byte)0);
	    arp.setTargetHardwareAddress(hardAddr);
	    arp.setTargetProtocolAddress(targetIP);
	    ether.setPayload(arp);
	    
	    // Enqueue the ethernet packet whose next mac address is not available
	    // send the arp request and wait for the mac address
	    arpObj.insert(targetIP, etherPacket);
	    this.sendPacket(ether, inIface);
	    arpObj.timeout(targetIP, this, ether, inIface);
	}
    }

    private void handleIpPacket(Ethernet etherPacket, Iface inIface)
    {
	// Make sure it's an IP packet
	if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
	{ return; }

	// Get IP header
	IPv4 ipPacket = (IPv4)etherPacket.getPayload();
	System.out.println("Handle IP packet");

	// Verify checksum
	short origCksum = ipPacket.getChecksum();
	ipPacket.resetChecksum();
	byte[] serialized = ipPacket.serialize();
	ipPacket.deserialize(serialized, 0, serialized.length);
	short calcCksum = ipPacket.getChecksum();
	if (origCksum != calcCksum)
	{ return; }

	// Check TTL
	ipPacket.setTtl((byte)(ipPacket.getTtl()-1));
	if (0 == ipPacket.getTtl()) { 
	    // create the ICMP message here and return
	    createICMPMessage(etherPacket, inIface, (byte)11, (byte)0); 
	    return; 
	}

	// Reset checksum now that TTL is decremented
	ipPacket.resetChecksum();

	// Check if packet is destined for one of router's interfaces
	boolean flag = false;
	for (Iface iface : this.interfaces.values()) {
	    if (ipPacket.getDestinationAddress() == iface.getIpAddress()) { 
		
		// TODO: Do echo reply
		// Uncomment the code below
		return;
		//flag = true;
	    }
	}

	// TODO: Echo reply implementation
	//if (flag) {
	//    for (Iface iface : this.interfaces.values()) {
	//	if (ipPacket.getDestinationAddress() != iface.getIpAddress()) { 
	//	    processPacketSentToRouter(etherPacket, inIface);   
	//	}
	//    }
	//    return;
	//} 

	// Do route lookup and forward
	this.forwardIpPacket(etherPacket, inIface);
	System.out.println("Done forwarding the packet");
    }

    private void processPacketSentToRouter(Ethernet etherPacket, Iface inIface) {
	System.out.println("In ProcessPacketSentToRouter");
	IPv4 ipPacket = (IPv4)etherPacket.getPayload();
	byte protocol = ipPacket.getProtocol();
	if (protocol == IPv4.PROTOCOL_UDP || protocol == IPv4.PROTOCOL_TCP) {
	    createICMPMessage(etherPacket, inIface, (byte)3, (byte)3); 
	} else if (protocol == IPv4.PROTOCOL_ICMP) {
	    ICMP icmp = (ICMP)ipPacket.getPayload();
	    if (icmp.getIcmpType() == ICMP.TYPE_ECHO_REQUEST ){
		processEchoRequest(etherPacket, inIface, (byte)0, (byte)0);
	    }
	}
    }

    private void processEchoRequest(Ethernet etherPacket, Iface inIface, byte type, byte code) {

	System.out.println("Inside processEchoRequest");

	Ethernet ether = createEthernetPacket(etherPacket, inIface);

	if (null == ether) {
	    System.out.println("Cannot create ether packet. Returning from process" +
		    "EchoRequest");
	    return;
	}

	// creating IPv4 Packet
	IPv4 ipPacket = (IPv4)etherPacket.getPayload();
	IPv4 ip = new IPv4();

	// intialize ttl
	ip.setTtl((byte)(64));

	// set protocol
	ip.setProtocol(IPv4.PROTOCOL_ICMP);

	int sourceAddress = ipPacket.getSourceAddress();

	// Find IP address of the inIface and set it as source Ip of the new packet
	ip.setSourceAddress(ipPacket.getDestinationAddress());

	// find source ip of the original packet and set it as destination
	ip.setDestinationAddress(sourceAddress);

	ICMP icmp = new ICMP();
	icmp.setIcmpType(type);
	icmp.setIcmpCode(code);

	icmp.setPayload(ipPacket.getPayload());
	ip.setPayload(icmp);
	ether.setPayload(ip);

	this.sendPacket(ether, inIface);
    }

    private void forwardIpPacket(Ethernet etherPacket, Iface inIface)
    {
	// Make sure it's an IP packet
	if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
	{ return; }
	System.out.println("Forward IP packet");

	// Get IP header
	IPv4 ipPacket = (IPv4)etherPacket.getPayload();
	int dstAddr = ipPacket.getDestinationAddress();

	// Find matching route table entry 
	RouteEntry bestMatch = this.routeTable.lookup(dstAddr);

	// If no entry matched, do nothing
	if (null == bestMatch) { 
	    createICMPMessage(etherPacket, inIface, (byte)3, (byte)0); 
	    return; 
	}

	// Make sure we don't sent a packet back out the interface it came in
	Iface outIface = bestMatch.getInterface();
	if (outIface == inIface)
	{ return; }

	// Set source MAC address in Ethernet header
	etherPacket.setSourceMACAddress(outIface.getMacAddress().toBytes());

	// If no gateway, then nextHop is IP destination
	int nextHop = bestMatch.getGatewayAddress();
	if (0 == nextHop)
	{ nextHop = dstAddr; }

	// Set destination MAC address in Ethernet header
	ArpEntry arpEntry = this.arpCache.lookup(nextHop);
	if (null == arpEntry){ 
	    handleARPPacket(etherPacket, inIface, ARP.OP_REPLY);
	    return; 
	}
	etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());
	this.sendPacket(etherPacket, outIface);
    }

    public void createICMPMessage(Ethernet origPacket, Iface inIface, byte type, byte code) {
	// create new ethernet packet
	Ethernet ether = createEthernetPacket(origPacket, inIface);

	if (null == ether) {
	    System.out.println("Cannot create ether packet. Returning from create"
		    + "ICMPMessage");
	    return;
	}

	IPv4 ip = createIPv4Packet(origPacket, inIface);

	if (null == ip) {
	    System.out.println("Cannot create ip packet. Returning from create"
		    + "ICMPMessage");
	    return;
	}

	ICMP icmp = createICMPPacket(origPacket, type, code);
	ether.setPayload(ip);
	ip.setPayload(icmp);
	this.sendPacket(ether, inIface);
    }

    private ICMP createICMPPacket(Ethernet origPacket, byte type, byte code) {

	ICMP icmp = new ICMP();
	icmp.setIcmpType(type);
	icmp.setIcmpCode(code);

	Data data = new Data();

	// Getting original 
	IPv4 ipPacket = (IPv4)origPacket.getPayload();
	int headerBytes = ipPacket.getHeaderLength() * 4;
	byte [] ipheaderbytes = ipPacket.serialize(); 

	// assigning 4 bytes buffer + headerBytes + 8 bytes buffer
	byte [] payload = new byte[4 + headerBytes + 8];	

	// Fill the payload array
	Arrays.fill(payload, 0, 4, (byte)0);
	for (int i = 0; i < headerBytes + 8; i++) {
	    payload[i + 4] = ipheaderbytes[i];
	}

	// set payload for the Data class
	data.setData(payload);

	// set payload for icmp packet
	icmp.setPayload(data);
	return icmp;
    }


    private IPv4 createIPv4Packet(Ethernet origPacket, Iface inIface) {
	// create new IPv4 Packet
	IPv4 ip = new IPv4();

	// intialize ttl
	ip.setTtl((byte)(64));

	// set protocol
	ip.setProtocol(IPv4.PROTOCOL_ICMP);

	// Find IP address of the inIface and set it as source Ip of the new packet
	int srcAddr = inIface.getIpAddress();
	ip.setSourceAddress(srcAddr);

	// find source ip of the original packet and set it as destination
	IPv4 ipPacket = (IPv4)origPacket.getPayload();
	int destAddr = ipPacket.getSourceAddress();
	ip.setDestinationAddress(destAddr);

	return ip;
    }

    private Ethernet createEthernetPacket(Ethernet origPacket, Iface inIface) {
	// create new ethernet packet
	Ethernet ether = new Ethernet();

	// set the type of packet
	ether.setEtherType(Ethernet.TYPE_IPv4);

	// source mac of the packet - interface on which we received initially
	ether.setSourceMACAddress(inIface.getMacAddress().toBytes());	

	// find the mac to forward to using the original ethernet
	// packet source ip address
	IPv4 ipPacket = (IPv4)origPacket.getPayload();
	int srcAddr = ipPacket.getSourceAddress();
	RouteEntry bestmatch = this.routeTable.lookup(srcAddr);

	// If no entry matched, do nothing
	if (null == bestmatch) { 
	    System.out.println("Cannot find the source address in the route table");
	    return null; 
	}

	// If no gateway, then nextHop is IP destination
	int nextHop = bestmatch.getGatewayAddress();
	if (0 == nextHop) { 
	    nextHop = srcAddr; 
	}

	// Set destination MAC address in Ethernet header
	ArpEntry arpEntry = this.arpCache.lookup(nextHop);
	if (null == arpEntry) { 
	    System.out.println("Cannot find arp entry");
	    return null; 
	}

	ether.setDestinationMACAddress(arpEntry.getMac().toBytes());
	return ether;
    }

}
