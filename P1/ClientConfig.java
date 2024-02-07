/**
 * @author Mrigank Kumar
 * 
 * Represents configuration parameters for an Iperfer client
 *
 * This record represents a configuration for an Iperfer client, including
 * the hostname of the server, the port number of the server,
 * and the test duration
 *
 * @param hostname   the hostname of the server to connect to
 * @param serverPort the port number of the server
 * @param time       the duration for the network test
 */
public record ClientConfig(String hostname, int serverPort, int time) {}
