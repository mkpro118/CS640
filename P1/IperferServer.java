import java.net.ServerSocket;

/**
 * @author Arushi Mishra
 *
 * This class represents the server side of the IPerfer network test
 * It extends the NetworkTest class and implements the logic for the
 * server functionality
 */
public class IperferServer extends NetworkTest {
    // Server configuration object
    ServerConfig config;

    // Output format
    private final static String serverFormat = "received=%d KB rate=%f Mbps\n";

    /**
     * Constructs an IperferServer object with the given server configuration
     *
     * @param config The server configuration to use.
     */
    public IperferServer(ServerConfig config) {
        super(serverFormat);
        this.config = config;
    }

    /**
     * Starts the server session for handling incoming client connections
     * and conducting tests
     */
    @Override
    public void startSession() {
        // make a server socket
        ServerSocket serverSocket = ConnectionUtils.createSocket(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConnectionUtils.closeSocket(serverSocket);
            System.out.flush();
        }));

        // accept a socket connection forever
        for(;;) {
            socket = ConnectionUtils.getClient(serverSocket);
            startTest();
        }
    }

    /**
     * Starts the network test
     * Measures the data reception rate from the client
     */
    @Override
    public void startTest() {
        // keep track of the start time
        startTime = System.currentTimeMillis();
        int temp = 0;
        totalBytes = 0;

        // keep receiving data until client connection is lost
        do {
            totalBytes += temp;
        } while ((temp = ConnectionUtils.receiveData(socket)) != -1);

        // keep track of the end time
        endTime = System.currentTimeMillis();

        // close socket connection
        stopSession();

        // print stats
        printSummary();

    }

}
