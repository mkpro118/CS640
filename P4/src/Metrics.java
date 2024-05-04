/**
 * @author Mrigank Kumar
 *
 * A data class for metrics.
 */
public class Metrics {
    /* Amount of Data transferred/received */
    public int dataTransferred;

    /* Number of packets sent/received */
    public int packetsTransferred;

    /* Number of out-of-sequence packets discarded */
    public int packetsDiscarded;

    /* Number of packets discarded due to incorrect checksum */
    public int wrongChecksum;

    /* Number of retransmissions */
    public int retransmissionCount;

    /* Number of duplicate acknowledgements */
    public int duplicateAckCount;

    /* Horizontal line for output isolation */
    private final static String HR;

    /* Output format */
    private final static String FORMAT;

    static {
        HR = "-".repeat(80) + "\n";
        FORMAT = "\n" + HR + "Amount of Data transferred/received: %d\n"
               + "Number of packets sent/received: %d\n"
               + "Number of out-of-sequence packets discarded: %d\n"
               + "Number of packets discarded due to incorrect checksum: %d\n"
               + "Number of retransmissions: %d\n"
               + "Number of duplicate acknowledgements: %d\n" + HR;
    }

    public Metrics() {
        dataTransferred = 0;
        packetsTransferred = 0;
        packetsDiscarded = 0;
        wrongChecksum = 0;
        retransmissionCount = 0;
        duplicateAckCount = 0;
    }

    @Override
    public String toString() {
        return String.format(FORMAT,
            dataTransferred,
            packetsTransferred,
            packetsDiscarded,
            wrongChecksum,
            retransmissionCount,
            duplicateAckCount
        );
    }
}
