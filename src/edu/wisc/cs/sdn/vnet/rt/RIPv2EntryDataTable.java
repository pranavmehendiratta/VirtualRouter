package edu.wisc.cs.sdn.vnet.rt;

import net.floodlightcontroller.packet.*;
import java.util.*;
import java.util.concurrent.*;

public class RIPv2EntryDataTable implements Runnable {
    private ConcurrentHashMap<RIPv2Entry, RIPv2EntryData> ripDataTable;
    private Thread timeoutThread; 
    private RouteTable routeTable;
    public static final long TIMEOUT = 30000; // 30 seconds

    public RIPv2EntryDataTable(RouteTable rt) {
	ripDataTable = new ConcurrentHashMap<>();
	timeoutThread = new Thread(this);
	timeoutThread.start();
	routeTable = rt;
    }

    public void insert(RIPv2Entry entry, int metric) {
	System.out.println("----- Inside insert -----");
	RIPv2Entry matchedEntry = checkIfEntryExists(null, entry);

	System.out.println("Entry: " + entry + ", matchedEntry: " + matchedEntry);
	
	if (matchedEntry == null) {
	    ripDataTable.put(entry, new RIPv2EntryData(metric));
	} else {
	    ripDataTable.get(matchedEntry).update(metric); 
	    // update route table as well
	    
	}    
	System.out.println("----- Done with insert -----");
    }

    public RIPv2EntryData getEntryData(RIPv2Entry entry) {
	if (!ripDataTable.containsKey(entry)) {
	    return ripDataTable.get(entry);
	}
	return null;
    }

    public void run() {
	while(true) {
	    try {
		Thread.sleep(1000); // Try deleting old entries after 1 sec
	    } catch (InterruptedException e) {
		// Do nothing here 
	    }

	    // Try removing the entries
	    for (RIPv2Entry entry : ripDataTable.keySet()) {
		RIPv2EntryData red = ripDataTable.get(entry);
		if (System.currentTimeMillis() - red.time >= TIMEOUT) {
		    //ripDataTable.remove(entry);
		    
		    // remove from route table as well
		    //this.routeTable.remove(entry.getAddress(), entry.getSubnetMask());
		}
	    }


	}
    }

    public void print() {
	System.out.println(Arrays.asList(ripDataTable));
    }

    public RIPv2Entry checkIfEntryExists(RouteEntry re, RIPv2Entry e1) {
	if (null == re && e1 == null) {
	    return null;
	}
	
	//print();

	if (re != null) {
	    for (RIPv2Entry entry : ripDataTable.keySet()) {
		if (entry.getAddress() == re.getDestinationAddress()
			&& entry.getSubnetMask() == re.getMaskAddress()) {
		    return entry; 
		}
	    }
	} else if (e1 != null) {
	    for (RIPv2Entry entry : ripDataTable.keySet()) {
		if (entry.getAddress() == e1.getAddress()
			&& entry.getSubnetMask() == e1.getSubnetMask()) {
		    return entry; 
		}
	    }
	}

	return null;
    }
}

class RIPv2EntryData {
    int metric;
    long time;

    public RIPv2EntryData(int metric) {
	this.metric = metric;
	this.time = System.currentTimeMillis();
    }

    public void update(int metric) {
	time = System.currentTimeMillis();
	if (this.metric > metric) {
	    this.metric = metric;
	}
    }

    public String toString() {
	return "metric: " + metric + ", time: " + time; 
    }
}

