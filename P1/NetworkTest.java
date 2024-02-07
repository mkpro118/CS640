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

    // The output stream for sending data
    protected OutputStream out;

    // The input stream for receiving data
    protected InputStream in;

    // The total number of bytes sent or received during the test
    protected long totalBytes;

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
     * This method should be implemented by subclasses to print information
     * about the network connection
     */
    public abstract void printSummary();

    /**
     * Prints a summary of the network test results
     * This method calculates and prints the total data sent or received during
     * the test session and the corresponding data transfer rate
     *
     * @param startTime the start time of the test session in milliseconds
     * @param endTime   the end time of the test session in milliseconds
     */
    protected NetworkTestStats getStats(long startTime, long endTime) {
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
