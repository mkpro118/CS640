package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import edu.wisc.cs.sdn.vnet.utils.TimeoutMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{	
    private TimeoutMap<MACAddress, Iface> cache;
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
        cache = new TimeoutMap<MACAddress, Iface>(15000L, 1000L);
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
		/* TODO: Handle packets                                             */

        MACAddress srcMAC = etherPacket.getSourceMAC();
        MACAddress destMAC = etherPacket.getDestinationMAC();

        cache.putTimed(srcMAC, inIface);

        Iface outIface = cache.getTimed(destMAC);

        // If dest Iface exists in cache, send it there
        if (outIface != null) {
            sendPacket(etherPacket, outIface);
            return;
        }

        // Flood the packet otherwise
        interfaces
        .values()
        .parallelStream()
        .filter(e -> e != null)
        .filter(e -> !e.getName().equals(inIface.getName()))
        .forEach(iface -> sendPacket(etherPacket, iface));
		
		/********************************************************************/
	}
}
