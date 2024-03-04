package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.MACAddress;

import static net.floodlightcontroller.packet.Ethernet.TYPE_IPv4;

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
		/********************************************************************/
		/* Handle packets                                             */

        // Check if packet if of type IPv4
        if (TYPE_IPv4 != etherPacket.getEtherType()) {
            System.out.println("NOT IPv4,dropping");
            return;
        }
        System.out.println("*** -> Received packet: " +
                etherPacket.toString().replace("\n", "\n\t"));

        // Get frame's payload
        IPv4 packet = (IPv4) etherPacket.getPayload();

        // Verify packet's checksum, if invalid, drop it
        if (!isChecksumValid(packet)) {
            System.out.println("checksum invalid, dropping");
            return;
        }

        // Check pre-decrement TTL, if not greater than 1, drop it
        if (packet.getTtl() <= 1) {
            System.out.println("TTL expired, dropping");
            return;
        }

        // Decrement TTL
        packet.setTtl((byte) (packet.getTtl() - 1));

        // If packet was meant for router, drop it
        if (isPacketForRouter(packet)) {
            System.out.println("Packet is for router, dropping");
            return;
        }

        RouteEntry entry;
        // If no matching entry, drop the packet
        if (null == (entry = routeTable.lookup(packet.getDestinationAddress()))) {
            System.out.println("No RouteEntry, dropping");
            return;
        }
        System.out.println("Found route entry " + entry);

        Iface outIface;
        // If destination is on the incoming interface, there might be a loop.
        // Drop the packet
        if ((outIface = entry.getInterface()).getName().equals(inIface.getName())) {
            System.out.println("Loop, dropping");
            return;
        }

        ArpEntry srcEntry;
        // If no matching entry, drop the packet
        if(null == (srcEntry = arpCache.lookup(outIface.getIpAddress()))) {
            System.out.println("No matching Source ArpEntry, drop");
            return;
        }

        // Get next hop's ip address. If it's zero, next hop is the destination
        int next;
        if ((next = entry.getGatewayAddress()) == 0)
            next = packet.getDestinationAddress();
        System.out.println("Next = " + next);

        ArpEntry destEntry;
        if(null == (destEntry = arpCache.lookup(next))) {
            System.out.println("No matching Destination ArpEntry, drop");
            return;
        }

        // Set source MAC to the router's out interface's MAC
        etherPacket.setSourceMACAddress(srcEntry.getMac().toBytes());
        System.out.println("Source MAC: " + srcEntry.getMac());
        // Set destination MAC to the destination's MAC
        etherPacket.setDestinationMACAddress(destEntry.getMac().toBytes());
        System.out.println("Dest MAC: " + destEntry.getMac());

        // Send the packet on the out interface
        System.out.println("Sending packet on interface " + outIface);
        System.out.println("Sending packet: " + etherPacket);
        sendPacket(etherPacket, outIface);
		
		/********************************************************************/
	}

    /**
     * @author Mrigank Kumar
     *
     * Validate the checksum on the given IP packet
     *
     * @param  packet The IP packet to validate checksum for
     *
     * @return true if checksum is correct, false otherwise
     */
    private boolean isChecksumValid(IPv4 packet) {
        // Original checksum
        short checksum = packet.getChecksum();

        // Set packet.checksum to 0 because packet.serialize() recomputes the
        // checksum over the header if the checksum field is set to zero
        packet.resetChecksum();

        // The following call will update packet.checksum,
        // which can then be retrieved with packet.getChecksum()
        packet.serialize();

        // Check whether the original checksum equals the recomputed checksum
        return checksum == packet.getChecksum();
    }

    /**
     * Check if the IP packet's destination address is one of the router's
     * interfaces
     * If so, the router will drop the packet
     *
     * @param  packet The packet to check
     * @return true if the packet's destination address is one of the router's
     *         interfaces, false otherwise
     */
    private boolean isPacketForRouter(IPv4 packet) {
        // IP packet's destination IP address
        int dest = packet.getDestinationAddress();

        // Evaluates to true if any of the router's interfaces' IP address
        // matches the destination IP address
        return interfaces        // interfaces is a Map<String, Iface>
               .values()          // We only consider the Iface values
               .parallelStream()  // Try to process in parallel
               .unordered()       // Order doesn't matter
               .anyMatch(iface -> dest == iface.getIpAddress());
    }
}
