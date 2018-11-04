package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.*;
import java.util.*;
import java.util.concurrent.*;

class ArpQOData {
    boolean request;
    int count;   
    Queue<Ethernet> packets;

    ArpQOData() {
	request = false;
	count = 1;
	packets = new LinkedList<>();
    }

    public void printPacketQueue() {
	for(Ethernet e : packets) { 
	    System.out.println(e); 
	}
    }

    public String toString() {
	return ("request: " + request + ", count: " + count);
    }
}


public class ArpQO {
    ConcurrentHashMap<Integer, ArpQOData> packetMap;
    
    public ArpQO() {
	packetMap = new ConcurrentHashMap<>();
    }

    public void insert(int ip, Ethernet etherPacket) {
	if (!packetMap.containsKey(ip)) {
	    packetMap.put(ip, new ArpQOData());
	} 
	packetMap.get(ip).packets.add(etherPacket);

    }
    
    public void timeout(final int ip, final Router router, final Ethernet ether, final Iface inIface) {
	final ArpQOData data = packetMap.get(ip);

	if (data.request) {
	    System.out.println("exiting in excute for ip: " + IPv4.fromIPv4Address(ip));
	    return;
	}
	
	if (data.count >= 3 && !data.request) {
	    // Desitnation net unreachable message if even after 3 requests
	    // mac address is not available

	    //data.printPacketQueue();

	    Ethernet packet = data.packets.remove();

	    System.out.println("packet being sent for ICMP request is");
	    System.out.println(packet.toString());

	    IPv4 ipPacket = (IPv4)packet.getPayload();
	    int srcAddr = ipPacket.getSourceAddress();
	    RouteEntry bestmatch = router.getRouteTable().lookup(srcAddr);

	    // If no entry matched, do nothing
	    if (null == bestmatch) { 
		System.out.println("Cannot find the source address in the route table");
		return; 
	    }   

	    Iface outIface = bestmatch.getInterface();
	    router.createICMPMessage(packet, outIface, (byte)3, (byte)1); 

	    if (data.packets.isEmpty()) {
		packetMap.remove(ip);
	    }
	    return;
	}

	new Timer().schedule(
		new TimerTask() {
		    public void run() {
			execute(ip, router, ether, inIface);
		}
	    }, 1000);
    }

    public void execute (int ip, Router router, Ethernet ether, Iface inIface) {
	ArpQOData data = packetMap.get(ip);

	if (data == null) {
	    return;
	}

	System.out.println("Attemp " + data.count  + "  at finding mac address"); 

	if (data.request) {
	    System.out.println("exiting in excute for ip: " + IPv4.fromIPv4Address(ip));
	    return;
	}

	if (!data.request && data.count < 3) {
	    router.sendPacket(ether, inIface);
	    data.count++;
	} 
	timeout(ip, router, ether, inIface);
    }

    public void print() {
	System.out.println("{");
	for (int ip : packetMap.keySet()) {
	    System.out.println("(" + IPv4.fromIPv4Address(ip) + " -> " + packetMap.get(ip).toString() + 
		    ")");
	}
	System.out.println("}");
    }
}
