import java.net.Socket;
import java.net.ServerSocket;

/**
 * This class represents the server side of the IPerfer network test
 * It extends the NetworkTest class and implements the logic for the
 * server functionality
 */
public class IperferServer extends NetworkTest {
    // Server configuration object
    ServerConfig config;

    // Output format
    private final static String serverFormat = "received=%d KB rate=%f Mbps\n";

    // Server socket
    private ServerSocket server;

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
        server = ConnectionUtils.createSocket(config);

        // Basic logging
        System.out.printf("Listening for TCP connections on Port: %d\n"
            + "Using Chunk Size: %d\n",
            config.listenPort(),
            Constants.CHUNK_SIZE.getValue());

        do { // Serve Forever
            System.out.println();
            socket = ConnectionUtils.getClient(server);
            startTest();
            stopSession();
            printSummary();
        } while (true); // Serve Forever
    }

    /**
     * Starts the network test
     * Measures the data reception rate from the client
     */
    @Override
    public void startTest() {
        startTime = System.currentTimeMillis();

        int bufSize = 0;


        // reset totalBytes
        totalBytes = 0;

        while (bufSize >= 0) {
            totalBytes += bufSize;
            bufSize = ConnectionUtils.receiveData(socket);
        }

        endTime = System.currentTimeMillis();
    }
}
