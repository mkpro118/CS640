/**
 * @author Mrigank Kumar
 *
 * A configuration record for client/sender
 */
public record SendConfig(int port, String remoteIP, int remotePort,
                         String fileName, int mtu, int sws) {}
