package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.*;
import java.util.*;
import java.util.concurrent.*;

class ArpQOData implements Runnable{ // implements Runnable {
    boolean request;
    int count;   
    Queue<Ethernet> packets;
    Iface outIface;
    Ethernet arpRequest;
    private Router router;
    //Thread attemptThread;

    // Thread setup parameters
    //Router router;
    //Ethernet arpRequest;
    //Iface iface;

    //public void initThreadParamters(Router router, Ethernet arpRequest, Iface inIface) {
    //    this.router = router;
    //    this.arpRequest = arpRequest;
    //    this.iface = inIface;
    //}

    //public void run() {
    //    System.out.println("ArpQOData thread execute function");	
    //}

    ArpQOData(Router router) {
        request = false;
        count = 0;
        packets = new ConcurrentLinkedQueue<>();
        this.router=router;
        //attemptThread = new Thread(this);
    }

    public void printPacketQueue() {
        for(Ethernet e : packets) { 
            System.out.println(e); 
        }
    }

    public String toString() {
        return ("request: " + request + ", count: " + count);
    }

    @Override
    public void run() {
        while(count < 3 && !request) {
            count++;
	        System.out.println("Sending Request No: " + count);      
            router.sendPacket(arpRequest, outIface);
            try {
                Thread.sleep(1000); 
            } catch (InterruptedException e) {

            }
        }

        if(!request)
            this.createICMP();

    }

    public void createICMP() {
        if(count < 3 && !request) {
            Ethernet packet = packets.remove();
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
            
            int dstAddr = ipPacket.getDestinationAddress();
            if (packets.isEmpty() && ArpQO.packetMap.containsKey(dstAddr)) {
                ArpQO.packetMap.remove(dstAddr);
            }
            return;
        }
    }
}

//class DataThreadObject extends ArpQOData {
//    Thread attemptThread;
//
//}

public class ArpQO { // implements Runnable {
    static ConcurrentHashMap<Integer, ArpQOData> packetMap;
    //ConcurrentHashMap<Integer, Thread> threadMap;

//    public ArpQO() {
//        packetMap = new ConcurrentHashMap<>();
//        //attemptThread = new Thread(this);
//        // this.router = r;
//    }

    static public void insert(int ip, Ethernet etherPacket, Ethernet ether, Iface bestMatch, Router router) {
        if(packetMap == null)
            packetMap = new ConcurrentHashMap<>();
        
        if (!packetMap.containsKey(ip)) {
            packetMap.put(ip, new ArpQOData(router));
        } 
        ArpQOData data = packetMap.get(ip);
        data.packets.add(etherPacket);
        data.outIface = bestMatch;
        data.arpRequest = ether;
        data.run();
    }

    //    public void execute1(int ip, Ethernet ether, Iface inIface) {
    //        // start the thead for ip ArpQOData object
    //        final ArpQOData data = packetMap.get(ip);
    //
    //
    //        //data.initThreadParamters(router, ether, inIface);
    //        //data.attemptThread.start();
    //    }
    //
    //    public void Run() {
    // TODO: Figure out how to send ip here to get the data object

    //final ArpQOData data = packetMap.get(ip);
    //
    //// Thread will stop after 3 tries
    //while (count < 4) {
    //    System.out.println("ARP request attempt: " + data.count);
    //    
    //    
    //   
    //    

    //    // Send ARP request every seonds
    //    count++;
    //    try {
    //	Thread.sleep(1000); 
    //    } catch (InterruptedException e) {
    //	
    //    }
    //}

    // If request for IP is still not fulfilled remove it
    // remove from packetsMap
//}

/*

    public void timeout(final int ip, final Router router, final Ethernet ether, final Iface inIface) {

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
 */
public static void print() {
    System.out.println("---------------- ARP Table -------------------");
    for (int ip : packetMap.keySet()) {
        System.out.println(IPv4.fromIPv4Address(ip) + " -> " + packetMap.get(ip).toString());
    }
    System.out.println("----------------------------------------------");
}

}