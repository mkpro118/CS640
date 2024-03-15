package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import edu.wisc.cs.sdn.vnet.utils.PeriodicTask;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.MACAddress;
import net.floodlightcontroller.packet.RIPv2Entry;
import net.floodlightcontroller.packet.UDP;

import static net.floodlightcontroller.packet.Ethernet.TYPE_IPv4;
import static net.floodlightcontroller.packet.IPv4.PROTOCOL_UDP;
import static net.floodlightcontroller.packet.MACAddress.MAC_ADDRESS_LENGTH;
import static net.floodlightcontroller.packet.RIPv2.COMMAND_REQUEST;
import static net.floodlightcontroller.packet.RIPv2.COMMAND_RESPONSE;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;

    /** Using RIP to dynamically configure Route Tables */
    private boolean rip;

    /** Using RIP to dynamically configure Route Tables */
    private PeriodicTask ripSender;

    /** Using RIP to dynamically configure Route Tables */
    private PeriodicTask cleaner;

    private boolean initialRIPResponse;

    /** RIP Destination IP is 224.0.0.9 */
    private final static int RIP_DEST_IP;

    /** RIP Destination MAC is FF:FF:FF:FF:FF:FF */
    private final static byte[] RIP_DEST_MAC;

    static {
        /** convert 224.0.0.9 to it's int value */
        RIP_DEST_IP = (224 << 24) | 9;

        /** convert FF:FF:FF:FF:FF:FF to byte[] value */
        RIP_DEST_MAC = new byte[MAC_ADDRESS_LENGTH];
        for (int i = 0; i < RIP_DEST_MAC.length; i++) {
            RIP_DEST_MAC[i] = (byte) 0xFF;
        }
    }
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
        rip = false;
        ripSender = new PeriodicTask(this::broadcastRIPResponse, 10000L, true);
        cleaner = new PeriodicTask(this::clearStaleEntries, 10000L, true);
	}

    /**
     * Allows this router to generate routing tables dynamically using the
     * RIP protocol
     */
    public void enableRIP() {
        if (rip) { return; }

        rip = true;
        ripSender.stop();

        routeTable.clear();

        initializeRouteTable();
        ripSender.start();
    }

    /**
     * Disables dynamic routing.
     */
    public void disableRIP() {
        rip = false;

        ripSender.stop();
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

        // Check if packet if of type IPv4
        if (TYPE_IPv4 != etherPacket.getEtherType())
            return;

        // Get frame's payload
        IPv4 packet = (IPv4) etherPacket.getPayload();

        // Verify packet's checksum, if invalid, drop it
        if (!isChecksumValid(packet))
            return;

        // Handle unsolicited RIP packet
        if (rip && packet.getProtocol() == PROTOCOL_UDP) {
            // (new Thread(() -> {
            //     handleRIP((RIPv2) packet.getPayload().getPayload());
            // })).start();
            handleRIP((RIPv2) packet.getPayload().getPayload());
            return;
        }

        // Check pre-decrement TTL, if not greater than 1, drop it
        if (packet.getTtl() <= 1)
            return;

        // Decrement TTL
        packet.setTtl((byte) (packet.getTtl() - 1));

        // If packet was meant for router, drop it
        if (isPacketForRouter(packet))
            return;

        RouteEntry entry;
        // If no matching entry, drop the packet
        if (null == (entry = routeTable.lookup(packet.getDestinationAddress())))
            return;

        Iface outIface;
        // If destination is on the incoming interface, there might be a loop.
        // Drop the packet
        if ((outIface = entry.getInterface()) == inIface)
            return;

        // Get next hop's ip address. If it's zero, next hop is the destination
        int next;
        if ((next = entry.getGatewayAddress()) == 0)
            next = packet.getDestinationAddress();

        ArpEntry destEntry;
        if(null == (destEntry = arpCache.lookup(next)))
            return;

        // Set source MAC to the router's out interface's MAC
        etherPacket.setSourceMACAddress(outIface.getMacAddress().toBytes());
        // Set destination MAC to the destination's MAC
        etherPacket.setDestinationMACAddress(destEntry.getMac().toBytes());

        packet.resetChecksum();
        // etherPacket.setPayload(packet);
        // Send the packet on the out interface
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

    private void broadcastRIPResponse() {
        // TODO:
    }

    private void initializeRouteTable() {
        interfaces        // interfaces is a Map<String, Iface>
        .values()          // We only consider the Iface values
        .parallelStream()
        .forEach(iface -> {
            routeTable.insert(
                iface.getIpAddress() & iface.getSubnetMask(), /* destination IP */
                0, /* Gateway Address */
                iface.getSubnetMask(), /* subnet mask */
                iface, /* Interface to send on */
                0 /* Cost of the link */
            );
        });
        broadcastRIPRequest();
    }

    private void broadcastRIPRequest() {
        interfaces        // interfaces is a Map<String, Iface>
        .values()          // We only consider the Iface values
        .parallelStream()
        .forEach(iface -> {
            sendPacket(generateRIPRequest(iface), iface);
        });
    }

    private Ethernet generateRIPRequest(Iface iface) {
        // Generate RIPv2 Request packet
        RIPv2 ripPacket = new RIPv2();
        ripPacket.setCommand(RIPv2.COMMAND_REQUEST);

        return encapsulateRIP(ripPacket, iface);
    }

    private Ethernet generateRIPResponse(Iface iface) {
        // Generate RIPv2 Request packet
        RIPv2 ripPacket = new RIPv2();
        ripPacket.setCommand(RIPv2.COMMAND_RESPONSE);

        routeTable
        .getEntries()
        .parallelStream()
        .forEach(entry -> {
            int address = entry.getDestinationAddress();
            int subnetMask = entry.getMaskAddress();
            int metric = entry.getCost();
            RIPv2Entry ripEntry = new RIPv2Entry(address, subnetMask, metric);

            ripEntry.setNextHopAddress(address);

            ripPacket.addEntry(ripEntry);
        });

        return encapsulateRIP(ripPacket, iface);
    }

    private Ethernet encapsulateRIP(RIPv2 ripPacket, Iface iface) {
        // Generate UDP packet for the RIP Request packet
        UDP udpPacket = new UDP();
        udpPacket.setSourcePort(UDP.RIP_PORT);
        udpPacket.setDestinationPort(UDP.RIP_PORT);
        udpPacket.setPayload(ripPacket);

        // Generate IP packet to carry the UDP packet
        IPv4 ipPacket = new IPv4();
        ipPacket.setSourceAddress(iface.getIpAddress());
        ipPacket.setDestinationAddress(RIP_DEST_IP);
        ipPacket.setProtocol(PROTOCOL_UDP);
        ipPacket.setPayload(udpPacket);

        // Generate Ethernet packet to carry the IP packet
        Ethernet etherPacket = new Ethernet();
        etherPacket.setEtherType(TYPE_IPv4);
        etherPacket.setDestinationMACAddress(RIP_DEST_MAC);
        etherPacket.setSourceMACAddress(iface.getMacAddress().toBytes());
        etherPacket.setPayload(ipPacket);

        // This should actually reset the UDP, IP
        // and Ethernet packet's checksums too
        ripPacket.resetChecksum();

        return etherPacket;
    }

    private void handleRIP(RIPv2 packet) {
        // TODO: Perform checks

        // Dispatch based on type
        switch (packet.getCommand()) {
        case COMMAND_REQUEST:
            handleRIPRequest(packet);
            break;
        case COMMAND_RESPONSE:
            handleRIPResponse(packet);
            break;
        default:
            System.err.println("Invalid RIP command type. Dropping packet");
        }
    }

    private void handleRIPResponse(RIPv2 packet) {
        // TODO:
    }

    private void handleRIPRequest(RIPv2 packet) {
        // TODO:
    }
}
