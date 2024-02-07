import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * @author Mrigank Kumar
 * 
 * Abstract class representing a network test.
 * This class defines the common structure and behavior of network test sessions
 */
public abstract class NetworkTest {
    protected record NetworkTestStats(long totalKB, double rate) {}

    // The socket used for communication
    protected Socket socket;

    // The start time of the network test
    protected long startTime;

    // The end time of the network test
    protected long endTime;

    // The total number of bytes sent or received during the test
    protected long totalBytes;

    // The format string to print out network statistics
    protected final String summaryFormat;

    /**
     * Default NetworkTest constructor, initializes the summary format string
     *
     * @param  format The summary format string
     */
    protected NetworkTest(String format) {
        summaryFormat = format;
    }

    /**
     * Starts a session for the network test
     * This method should be implemented by subclasses to perform any
     * necessary setup before starting the actual test
     */
    public abstract void startSession();

    /**
     * Stops the network test session
     */
    public void stopSession() {
        ConnectionUtils.closeSocket(socket);
    }

    /**
     * Starts the network test
     * This method should be implemented by subclasses to carry out the specific
     * network test functionality
     */
    public abstract void startTest();

    /**
     * Print network connection speed statistics
     */
    public void printSummary() {
        NetworkTestStats stats = getStats();

        System.out.printf(summaryFormat, stats.totalKB(), stats.rate());
    }

    /**
     * Calculate the total data sent or received during
     * the test session and the corresponding data transfer rate
     *
     * @return A NetworkTestStats instance with the calculated statistics
     */
    public NetworkTestStats getStats() {
        // Time delta in milliseconds
        double delta = endTime - startTime;

        // Duration in seconds
        double duration = delta / Constants.MILLISECONDS_IN_SECONDS.getValue();

        // Total data in kilobytes
        long totalKB = totalBytes / Constants.BYTES_IN_KB.getValue();

        // Rate in megabits per second
        double rate = (totalKB * Constants.BITS_IN_BYTE.getValue()) / duration;

        return new NetworkTestStats(totalKB, rate);
    }
}
