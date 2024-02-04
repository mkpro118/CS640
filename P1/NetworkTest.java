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
        double duration = (endTime - startTime) / 1000.0; // Duration in seconds
        double totalKB = totalBytes / 1000.0; // Total data in kilobytes
        double rate = (totalKB * 8) / duration; // Rate in megabits per second
        System.out.printf("sent=%f KB rate=%f Mbps\n", totalKB, rate);
    }
}
