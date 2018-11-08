package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.*;
import java.util.*;
import java.util.concurrent.*;

class ArpQOData implements Runnable {
    boolean request;
    int count;   
    Queue<Ethernet> packets;
    Iface outIface;
    Ethernet arpRequest;
    private Router router;

    ArpQOData(Router router) {
        request = false;
        count = 0;
        packets = new ConcurrentLinkedQueue<>();
        this.router=router;
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
                //System.out.println("Cannot find the source address in the route table");
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

public class ArpQO { 
    static ConcurrentHashMap<Integer, ArpQOData> packetMap;
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

public static void print() {
    System.out.println("---------------- ARP Table -------------------");
    for (int ip : packetMap.keySet()) {
        System.out.println(IPv4.fromIPv4Address(ip) + " -> " + packetMap.get(ip).toString());
    }
    System.out.println("----------------------------------------------");
}

}
