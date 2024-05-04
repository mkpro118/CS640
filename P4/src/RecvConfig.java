/**
 * @author Arushi Mishra
 *
 * A configuration record for server/receiver
 */
public record RecvConfig(int port, int mtu, int sws, String fileName) {}
