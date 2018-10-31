package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import java.util.Arrays;
import net.floodlightcontroller.packet.*;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
    /** Routing table for the router */
    private RouteTable routeTable;

    /** ARP cache for the router */
    private ArpCache arpCache;

    /**
     * Creates a router for a specific host.
     * @param host hostname for the router
     */
    public Router(String host, DumpFile logfile)
    {
	super(host,logfile);
	this.routeTable = new RouteTable();
	this.arpCache = new ArpCache();
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

	switch(etherPacket.getEtherType())
	{
	    case Ethernet.TYPE_IPv4:
		this.handleIpPacket(etherPacket, inIface);
		break;
		// Ignore all other packet types, for now
	}

	/********************************************************************/
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
	    // TODO
	    System.out.println("ttl is zero");
	    createICMPMessage(etherPacket, inIface); 
	    return; 
	}

	// Reset checksum now that TTL is decremented
	ipPacket.resetChecksum();

	// Check if packet is destined for one of router's interfaces
	for (Iface iface : this.interfaces.values())
	{
	    if (ipPacket.getDestinationAddress() == iface.getIpAddress())
	    { return; }
	}

	// Do route lookup and forward
	this.forwardIpPacket(etherPacket, inIface);
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
	if (null == bestMatch)
	{ return; }

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
	if (null == arpEntry)
	{ return; }
	etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());

	this.sendPacket(etherPacket, outIface);
    }
    
    private void createICMPMessage(Ethernet origPacket, Iface inIface) {
	System.out.println("-------- Inside create ICMP message ---------");
	
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

	ICMP icmp = createICMPPacket(origPacket);
	
	ether.setPayload(ip);
	ip.setPayload(icmp);

	this.sendPacket(ether, inIface);
	System.out.println("-------- Done with create ICMP message ---------");
    }

    private ICMP createICMPPacket(Ethernet origPacket) {
	System.out.println("------- Inside createICMPPacket --------");
	ICMP icmp = new ICMP();
	icmp.setIcmpType((byte)11);
	icmp.setIcmpCode((byte)0);
	
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
	//System.out.println("payload: " + payload.toString());

	// set payload for icmp packet
	icmp.setPayload(data);
	System.out.println("------- Done with createICMPPacket --------");
	return icmp;
    }


    private IPv4 createIPv4Packet(Ethernet origPacket, Iface inIface) {
	System.out.println("-------- Inside createIPv4Packet ---------");

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

	System.out.println("ttl is " + (int)(ip.getTtl()));
	System.out.println("Source ip: " + IPv4.fromIPv4Address(ip.getSourceAddress()));
	System.out.println("dest ip: " + IPv4.fromIPv4Address(ip.getDestinationAddress()));
	

	//ip.serialize();
	System.out.println("-------- Done with createIPv4Packet ---------");
	return ip;
    }

    private Ethernet createEthernetPacket(Ethernet origPacket, Iface inIface) {
	System.out.println("-------- Inside createEthernetPacket ---------");
	
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
	    System.out.println("Cannot find arpentry");
	    return null; 
	}

	ether.setDestinationMACAddress(arpEntry.getMac().toBytes());
	
	System.out.println("---------------------------------------");
	System.out.println("Ethernet type: " + ether.getEtherType());
	System.out.println("Source mac: " + ether.getSourceMAC().toString());
	System.out.println("Interface mac: " + inIface.getMacAddress().toString());
	System.out.println("Dest mac: " + ether.getDestinationMAC().toString());
	System.out.println("---------------------------------------");
	
	System.out.println("------- Done with createEthernetPacket ---------");
	return ether;
    }

}
