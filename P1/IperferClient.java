

/**
 * @author Arushi Mishra
 *
 * This class represents the client side of the IPerfer network test
 * It extends the NetworkTest class and implements the logic for the
 * client functionality
 */
public class IperferClient extends NetworkTest {
    // Client configuration
    ClientConfig config;

    // Output format
    private final static String clientFormat = "sent=%d KB rate=%f Mbps\n";

    /**
     * Constructs an IperferClient object with the given client configuration
     *
     * @param config The client configuration to use.
     */
    public IperferClient(ClientConfig config) {
        super(clientFormat);
        this.config = config;
    }

    /**
     * Starts the a test session for the client and runs a network test
     */
    @Override
    public void startSession() {
        // Implement client logic to send data

        //create socket connection
        socket = ConnectionUtils.createSocket(config);

        //start test
        startTest();

    }

    /**
     * Stops the session and prints the test summary
     */
    public void stopSession() {
        // Implement client termination logic

        //close the socket connection 
        super.stopSession();
        //print stat summary from NetworkTest
        printSummary();
    }

    /**
     * Starts the network test
     * This method constantly sends data to the server for the duration
     * specified by the client configuration instance
     */
    @Override
    public void startTest() {

        startTime = System.currentTimeMillis();
        endTime = startTime + 
            config.time() * Constants.MILLISECONDS_IN_SECONDS.getValue();

        //send data for config.time() seconds 
        while(System.currentTimeMillis() < endTime) {
            ConnectionUtils.sendData(socket);
            totalBytes += Constants.CHUNK_SIZE.getValue();
        }

        //stop session 
        stopSession();
    }
}
