import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;

public abstract class NetworkTest {
    protected Socket socket;
    protected OutputStream out;
    protected InputStream in;
    protected long totalBytes;

    public abstract void startTest();

    protected void printSummary(long startTime, long endTime) {
        // Time delta in milliseconds
        double delta = endTime - startTime;

        // Duration in seconds
        double duration = delta / Constants.MILLISECONDS_IN_SECONDS.getValue();

        // Total data in kilobytes
        double totalKB = (double) totalBytes / Constants.BYTES_IN_KB.getValue();

        // Rate in megabits per second
        double rate = (totalKB * Constants.BITS_IN_BYTE.getValue()) / duration;

        System.out.printf("sent=%f KB rate=%f Mbps\n", totalKB, rate);
    }
}
