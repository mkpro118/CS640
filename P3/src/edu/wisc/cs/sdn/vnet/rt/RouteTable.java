package edu.wisc.cs.sdn.vnet.rt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.floodlightcontroller.packet.IPv4;

import edu.wisc.cs.sdn.vnet.Iface;
import edu.wisc.cs.sdn.vnet.utils.PeriodicTask;

import static java.lang.Integer.MIN_VALUE;
import static java.lang.Integer.bitCount;

/**
 * Route table for a router.
 * @author Aaron Gember-Jacobson
 */
public class RouteTable 
{
	/** Time after which stale entries are removed */
	public static long timeDelta = 30000L;

	/** Entries in the route table */
	private List<RouteEntry> entries;

	/** Use RIP configuration */
	private boolean rip;

	/** Using RIP to dynamically configure Route Tables */
    private PeriodicTask cleaner;
	
	/**
	 * Initialize an empty route table.
	 */
	public RouteTable()
	{
		this.entries = new LinkedList<RouteEntry>();
		rip = false;
		cleaner = new PeriodicTask(this::clearStaleEntries, 10000L, true);
	}

	public List<RouteEntry> getEntries() { return entries; }

	/**
	 * Enable RIP configuration
	 */
	public void enableRIP() {
		rip = true;
		cleaner.start();
	}

	/**
	 * Disable RIP configuration
	 */
	public void disableRIP() {
		rip = false;
		cleaner.stop();
	}
	
	/**
	 * Lookup the route entry that matches a given IP address.
	 * @param ip IP address
	 * @return the matching route entry, null if none exists
	 */
	public RouteEntry lookup(int ip)
	{
		synchronized(this.entries)
		{
			/*****************************************************************/
			/* Find the route entry with the longest prefix match	  */
			
            // Track the most specific entry so far
            var bestEntry = new Object(){
            	public RouteEntry entry = null;

	            // Length of the longest prefix so far
            	public int prefixLength = MIN_VALUE;
            };

            try {
            // We attempt to perform a parallel search
            entries.parallelStream()
                   .unordered()
                   .forEach(entry -> {
               	// Subnet Number of the current entry
                int subnetMask = entry.getMaskAddress();

                // Subnet mask of the current entry
                int subnetNumber = entry.getDestinationAddress() & subnetMask;

                // Length of the current prefix
                int prefixLength;

                if ((ip & subnetMask) == subnetNumber) {
                    // Longest prefix possible is 32, if found, stop searching
                    if ((prefixLength = bitCount(subnetMask)) == 32) {
                        bestEntry.entry = entry;
                        throw new RuntimeException();
                    }

                    // Update longestPrefix and bestEntry if
                    // the current entry is more specific
                    if (prefixLength > bestEntry.prefixLength) {
                        bestEntry.prefixLength = prefixLength;
                        bestEntry.entry = entry;
                    }
                }
            });
            } catch (RuntimeException unused) {}

            return bestEntry.entry;
			/*****************************************************************/
		}
	}

	private void clearStaleEntries() {
		synchronized(entries) {
			final long curr = System.currentTimeMillis();
			Predicate<Long> timeout = lastUpdate -> curr - lastUpdate > timeDelta;

			entries.removeIf(entry -> {
				if (entry == null)
					return true;

				if (entry.isPermanent())
					return false;

				if (timeout.test(entry.getLastUpdate()))
					return true;

				return false;
			});
		}
	}
	
	/**
	 * Populate the route table from a file.
	 * @param filename name of the file containing the static route table
	 * @param router the route table is associated with
	 * @return true if route table was successfully loaded, otherwise false
	 */
	public boolean load(String filename, Router router)
	{
		// Open the file
		BufferedReader reader;
		try 
		{
			FileReader fileReader = new FileReader(filename);
			reader = new BufferedReader(fileReader);
		}
		catch (FileNotFoundException e) 
		{
			System.err.println(e.toString());
			return false;
		}
		
		while (true)
		{
			// Read a route entry from the file
			String line = null;
			try 
			{ line = reader.readLine(); }
			catch (IOException e) 
			{
				System.err.println(e.toString());
				try { reader.close(); } catch (IOException f) {};
				return false;
			}
			
			// Stop if we have reached the end of the file
			if (null == line)
			{ break; }
			
			// Parse fields for route entry
			String ipPattern = "(\\d+\\.\\d+\\.\\d+\\.\\d+)";
			String ifacePattern = "([a-zA-Z0-9]+)";
			Pattern pattern = Pattern.compile(String.format(
					"%s\\s+%s\\s+%s\\s+%s", 
					ipPattern, ipPattern, ipPattern, ifacePattern));
			Matcher matcher = pattern.matcher(line);
			if (!matcher.matches() || matcher.groupCount() != 4)
			{
				System.err.println("Invalid entry in routing table file");
				try { reader.close(); } catch (IOException f) {};
				return false;
			}

			int dstIp = IPv4.toIPv4Address(matcher.group(1));
			if (0 == dstIp)
			{
				System.err.println("Error loading route table, cannot convert "
						+ matcher.group(1) + " to valid IP");
				try { reader.close(); } catch (IOException f) {};
				return false;
			}
			
			int gwIp = IPv4.toIPv4Address(matcher.group(2));
			
			int maskIp = IPv4.toIPv4Address(matcher.group(3));
			if (0 == maskIp)
			{
				System.err.println("Error loading route table, cannot convert "
						+ matcher.group(3) + " to valid IP");
				try { reader.close(); } catch (IOException f) {};
				return false;
			}
			
			String ifaceName = matcher.group(4).trim();
			Iface iface = router.getInterface(ifaceName);
			if (null == iface)
			{
				System.err.println("Error loading route table, invalid interface "
						+ matcher.group(4));
				try { reader.close(); } catch (IOException f) {};
				return false;
			}
			
			// Add an entry to the route table
			this.insert(dstIp, gwIp, maskIp, iface);
		}
	
		// Close the file
		try { reader.close(); } catch (IOException f) {};
		return true;
	}
	
	/**
	 * Add an entry to the route table.
	 * @param dstIp destination IP
	 * @param gwIp gateway IP
	 * @param maskIp subnet mask
	 * @param iface router interface out which to send packets to reach the 
	 *		destination or gateway
	 */
	public void insert(int dstIp, int gwIp, int maskIp, Iface iface)
	{
		insert(dstIp, gwIp, maskIp, iface, RouteEntry.infinity, true);
	}

	/**
	 * Add an entry to the route table.
	 * @param dstIp destination IP
	 * @param gwIp gateway IP
	 * @param maskIp subnet mask
	 * @param iface router interface out which to send packets to reach the
	 *		destination or gateway
	 * @param cost cost of this link
	 * @param permanent whether or not this entry should be preserves at cleanup
	 */
	public void insert(int dstIp, int gwIp, int maskIp, Iface iface, int cost,
						boolean permanent)
	{
		RouteEntry entry = new RouteEntry(dstIp, gwIp, maskIp, iface);

		if (permanent)
			entry.makePermanent();

		if (cost < 0) {
			throw new IllegalArgumentException(
				"Cannot have negative costs on links!"
			);
		}

		entry.setCost(cost);

		synchronized(this.entries)
		{
			this.entries.add(entry);
		}
	}
	
	/**
	 * Remove an entry from the route table.
	 * @param dstIP destination IP of the entry to remove
	 * @param maskIp subnet mask of the entry to remove
	 * @return true if a matching entry was found and removed, otherwise false
	 */
	public boolean remove(int dstIp, int maskIp)
	{ 
		synchronized(this.entries)
		{
			RouteEntry entry = this.find(dstIp, maskIp);
			if (null == entry) { return false; }
			this.entries.remove(entry);
		}
		return true;
	}
	
	/**
	 * Update an entry in the route table.
	 * @param dstIP destination IP of the entry to update
	 * @param maskIp subnet mask of the entry to update
	 * @param gatewayAddress new gateway IP address for matching entry
	 * @param iface new router interface for matching entry
	 * @return true if a matching entry was found and updated, otherwise false
	 */
	public boolean update(int dstIp, int maskIp, int gwIp, Iface iface)
	{
		return update(dstIp, maskIp, gwIp, iface, RouteEntry.infinity);
	}

	/**
	 * Update an entry in the route table.
	 * @param dstIP destination IP of the entry to update
	 * @param maskIp subnet mask of the entry to update
	 * @param gatewayAddress new gateway IP address for matching entry
	 * @param iface new router interface for matching entry
	 * @param cost the cost to this entry
	 * @return true if a matching entry was found and updated, otherwise false
	 */
	public boolean update(int dstIp, int maskIp, int gwIp, Iface iface, int cost)
	{
		synchronized(this.entries)
		{
			RouteEntry entry = this.find(dstIp, maskIp);
			if (null == entry) { return false; }
			entry.setGatewayAddress(gwIp);
			entry.setInterface(iface);
			entry.setCost(cost);
		}
		return true;
	}

	/**
	 * Clears all entries in the route table.
	 */
	public void clear() {
		synchronized(this.entries) {
			this.entries.clear();
		}
	}

	/**
	 * Find an entry in the route table.
	 * @param dstIP destination IP of the entry to find
	 * @param maskIp subnet mask of the entry to find
	 * @return a matching entry if one was found, otherwise null
	 */
	private RouteEntry find(int dstIp, int maskIp)
	{
		synchronized(this.entries)
		{
			for (RouteEntry entry : this.entries)
			{
				if ((entry.getDestinationAddress() == dstIp)
					&& (entry.getMaskAddress() == maskIp)) 
				{ return entry; }
			}
		}
		return null;
	}
	
	public String toString()
	{
		synchronized(this.entries)
		{ 
			if (0 == this.entries.size())
			{ return " WARNING: route table empty"; }
			
			String result = "Destination\tGateway\t\tMask\t\tIface\n";
			for (RouteEntry entry : entries)
			{ result += entry.toString()+"\n"; }
			return result;
		}
	}
}
