package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import edu.wisc.cs.sdn.vnet.utils.PeriodicTask;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.IChecksum;
import net.floodlightcontroller.packet.MACAddress;
import net.floodlightcontroller.packet.RIPv2;
import net.floodlightcontroller.packet.RIPv2Entry;
import net.floodlightcontroller.packet.UDP;

import static net.floodlightcontroller.packet.Ethernet.TYPE_IPv4;
import static net.floodlightcontroller.packet.IPv4.PROTOCOL_UDP;
import static net.floodlightcontroller.packet.MACAddress.MAC_ADDRESS_LENGTH;
import static net.floodlightcontroller.packet.RIPv2.COMMAND_REQUEST;
import static net.floodlightcontroller.packet.RIPv2.COMMAND_RESPONSE;
import static net.floodlightcontroller.packet.UDP.RIP_PORT;

import java.util.Arrays;
import java.util.function.Function;

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
        ripSender = new PeriodicTask(() -> broadcastRIP(COMMAND_RESPONSE), 10000L, true);
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
        routeTable.disableRIP();
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

            handleRIP(etherPacket, inIface);
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
    private boolean isChecksumValid(IChecksum<?> packet) {
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

    private void initializeRouteTable() {
        // Enable RIP on the routing table
        routeTable.enableRIP();

        // Setup routing tables for all directly connected interfaces
        interfaces        // interfaces is a Map<String, Iface>
        .values()         // We only consider the Iface values
        .parallelStream()
        .forEach(iface -> {
            routeTable.insert(
/* dstIp   */   iface.getIpAddress() & iface.getSubnetMask(),
/* gwIp    */   0x00000000,
/* maskIp  */   iface.getSubnetMask(),
/* iface   */   iface,
/* cost    */   0x0,
/*permanent*/   true
            );
        });

        // Request information on startup
        broadcastRIP(COMMAND_REQUEST);
    }

    private void broadcastRIP(int type) {
        Function<Iface, Ethernet> generator;
        switch (type) {
        case COMMAND_REQUEST:
            generator = this::generateRIPRequest;
            break;
        case COMMAND_RESPONSE:
            generator = this::generateRIPResponse;
            break;
        default:
            System.err.println("Invalid broadcast type!");
            return;
        }

        interfaces        // interfaces is a Map<String, Iface>
        .values()         // We only consider the Iface values
        .parallelStream()
        .forEach(iface -> {
            sendPacket(generator.apply(iface), iface);
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
        udpPacket.setSourcePort(RIP_PORT)
                 .setDestinationPort(RIP_PORT)
                 .setPayload(ripPacket);

        // Generate IP packet to carry the UDP packet
        IPv4 ipPacket = new IPv4();
        ipPacket.setSourceAddress(iface.getIpAddress())
                .setDestinationAddress(RIP_DEST_IP)
                .setProtocol(PROTOCOL_UDP)
                .setPayload(udpPacket);

        // Generate Ethernet packet to carry the IP packet
        Ethernet etherPacket = new Ethernet();
        etherPacket.setEtherType(TYPE_IPv4)
                   .setDestinationMACAddress(RIP_DEST_MAC)
                   .setSourceMACAddress(iface.getMacAddress().toBytes())
                   .setPayload(ipPacket);

        // This should actually reset the UDP, IP
        // and Ethernet packet's checksums too
        ripPacket.resetChecksum();

        return etherPacket;
    }

    private void handleRIP(Ethernet etherPacket, Iface iface) {
        // Check the destination MAC address is the RIP MAC "FF:FF:FF:FF:FF:FF"
        if (!Arrays.equals(etherPacket.getDestinationMACAddress(), RIP_DEST_MAC))
            return;

        IPv4 ipPacket = (IPv4) etherPacket.getPayload();

        // Check the destination IP address is the RIP IP "224.0.0.9"
        if (ipPacket.getDestinationAddress() != RIP_DEST_IP)
            return;

        // Check underlying packet is UDP
        if (!(ipPacket.getPayload() instanceof UDP))
            return;

        UDP udpPacket = (UDP) ipPacket.getPayload();

        // UDP should have valid checksum
        if (!isChecksumValid(udpPacket))
            return;

        // RIP packet should have the UDP.RIP_PORT as the destination port
        if (udpPacket.getDestinationPort() != RIP_PORT)
            return;

        // Check underlying packet is RIPv2
        if (!(udpPacket.getPayload() instanceof RIPv2))
            return;

        RIPv2 ripPacket = (RIPv2) udpPacket.getPayload();

        // Dispatch based on type
        switch (ripPacket.getCommand()) {
        case COMMAND_REQUEST:
            handleRIPRequest(etherPacket, iface);
            break;
        case COMMAND_RESPONSE:
            handleRIPResponse(etherPacket, iface);
            break;
        default:
            System.err.println("Invalid RIP command type. Dropping packet");
        }
    }

    private void handleRIPRequest(Ethernet etherPacket, Iface iface) {
        Ethernet responseFrame = generateRIPResponse(iface);

        responseFrame.setDestinationMACAddress(etherPacket.getSourceMACAddress());

        IPv4 requestPacket = (IPv4) etherPacket.getPayload();
        IPv4 responsePacket = (IPv4) responseFrame.getPayload();
        responsePacket.setDestinationAddress(requestPacket.getSourceAddress());

        // Reset the RIP checksum, and that will reset all parent checksums
        responsePacket.getPayload().getPayload().resetChecksum();

        sendPacket(responseFrame, iface);
    }

    private void handleRIPResponse(Ethernet etherPacket, Iface iface) {
        IPv4 ipPacket = (IPv4) etherPacket.getPayload();
        RIPv2 ripPacket = (RIPv2) ipPacket.getPayload().getPayload();

        // While it is highly unlikely, in case we don't have a entry for
        // the interface this RIP Packet arrived on, then our assumed cost
        // is infinity
        int currCost = RouteEntry.infinity;

        RouteEntry current = routeTable.lookup(ipPacket.getSourceAddress());

        // It is highly unlikely the entry is null, but we check anyway
        if (current != null) {
            currCost = current.getCost();
        }

        // Add one to account for the very next hop on the interface this packet
        // arrived on.
        final int cost = currCost + 1;

        ripPacket
        .getEntries()
        .parallelStream()
        .unordered()
        .forEach(ripEntry -> {
            RouteEntry routeEntry = routeTable.lookup(ripEntry.getAddress());

            if (routeEntry != null) {
                // If we have a Route Entry for this RIP Entry, then use distance
                // vector's relax step to determine whether we should update

                if (ripEntry.getMetric() + cost <= routeEntry.getCost()) {
                    routeTable.update(
    /* dstIp  */        ripEntry.getAddress() & ripEntry.getSubnetMask(),
    /* maskIp */        ripEntry.getSubnetMask(),
    /* gwIp   */        ipPacket.getSourceAddress(),
    /* iface  */        iface,
    /* cost   */        ripEntry.getMetric() + cost
                    );
                }
            } else {
                // If we don't have an Route Entry for this RIP Entry
                // just add it as a temporary entry

                routeTable.insert(
 /* dstIp     */    ripEntry.getAddress() & ripEntry.getSubnetMask(),
 /* gwIp      */    ipPacket.getSourceAddress(),
 /* maskIp    */    ripEntry.getSubnetMask(),
 /* iface     */    iface,
 /* cost      */    ripEntry.getMetric() + cost,
 /* permanent */    false
                );
            }
        });
    }
}
