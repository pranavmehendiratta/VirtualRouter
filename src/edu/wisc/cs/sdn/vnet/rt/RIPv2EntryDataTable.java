package edu.wisc.cs.sdn.vnet.rt;

import net.floodlightcontroller.packet.*;
import java.util.*;
import java.util.concurrent.*;

public class RIPv2EntryDataTable implements Runnable {
    ConcurrentHashMap<String, RIPv2EntryData> ripDataTable;
    ConcurrentHashMap<String, Integer> routerInterfaces;
    private Thread timeoutThread; 
    public static final long TIMEOUT = 30000; // 30 seconds

    public RIPv2EntryDataTable(RouteTable rt) {
	ripDataTable = new ConcurrentHashMap<>();
	routerInterfaces = new ConcurrentHashMap<>();
	timeoutThread = new Thread(this);
	timeoutThread.start();
    }

    public boolean insert(String entry, int metric) {
	if (routerInterfaces.containsKey(entry)) {
	    return false;
	}
	
	if (!ripDataTable.containsKey(entry)) {
	    ripDataTable.put(entry, new RIPv2EntryData(metric));
	    return true;
	} else {
	    if (ripDataTable.get(entry).update(metric)) {
		return true;
	    }
	    return false;
	}    
    }

    public RIPv2EntryData getEntryData(RIPv2Entry entry) {
	if (!ripDataTable.containsKey(entry)) {
	    return ripDataTable.get(entry);
	}
	return null;
    }

    public void run() {
	int count = 0; // Just for testing
	while(true) {
	    try {
		Thread.sleep(1000); // Try deleting old entries after 1 sec
	    } catch (InterruptedException e) {
		// Do nothing here 
	    }

	    // Try removing the entries
	    for (String entry : ripDataTable.keySet()) {
		RIPv2EntryData red = ripDataTable.get(entry);
		if (System.currentTimeMillis() - red.time >= TIMEOUT) {
		    
		    if (!routerInterfaces.containsKey(entry)) { 
			System.out.println("Remove entry: " + entry);
		    }
		}
	    }
	}
    }

    public void print() {
	System.out.println("----- ripDataTable -----");
	System.out.println(Arrays.asList(ripDataTable));
	System.out.println("----- routersInterface -----");
	System.out.println(Arrays.asList(routerInterfaces));
    } 
}

class RIPv2EntryData {
    int metric;
    long time;

    public RIPv2EntryData(int metric) {
	this.metric = metric;
	this.time = System.currentTimeMillis();
    }

    public boolean update(int metric) {
	time = System.currentTimeMillis();
	if (this.metric > metric) {
	    this.metric = metric;
	    return true;
	}
	return false;
    }

    public String toString() {
	return "metric: " + metric + ", time: " + time; 
    }
}

