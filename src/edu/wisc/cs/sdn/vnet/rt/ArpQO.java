package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.*;
import java.util.*;
import java.util.concurrent.*;

class ArpQOData {
    boolean request;
    int count;   
    List<Ethernet> packets;

    ArpQOData() {
	request = false;
	count = 1;
	packets = new ArrayList<>();
    }

    public String toString() {
	return ("request: " + request + ", count: " + count);
    }
}


public class ArpQO {
    ConcurrentHashMap<Integer, ArpQOData> queue;
    
    public ArpQO() {
	queue = new ConcurrentHashMap<>();
    }

    public void insert(int ip, Ethernet etherPacket) {
	if (!queue.containsKey(ip)) {
	    queue.put(ip, new ArpQOData());
	} 
	queue.get(ip).packets.add(etherPacket);

    }
    
    public void timeout(final int ip, final Router router, final Ethernet ether, final Iface inIface) {
	final ArpQOData data = queue.get(ip);

	if (data.request) {
	    return;
	}
	
	if (data.count >= 3 && !data.request) {
	    // Removing the etherpackets whose ip the router still does not have
	    queue.remove(ip);
	    
	    // Desitnation net unreachable message if the even after 3 requests
	    // mac address is not available
	    router.createICMPMessage(ether, inIface, (byte)3, (byte)1); 
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
	ArpQOData data = queue.get(ip);
	
	if (data.request) {
	    return;
	}
	
	if (!data.request && data.count < 3) {
	    router.sendPacket(ether, inIface);
	    data.count++;
	} 
	timeout(ip, router, ether, inIface);
    }

    public void print() {
	System.out.println(Arrays.asList(queue));
    }

}
