public class Metrics {
    public int dataTransferred; // Amount of Data transferred/received
    public int packetsTransferred; // Number of packets sent/received
    public int packetsDiscarded; // Number of out-of-sequence packets discarded
    public int wrongChecksum; // Number of packets discarded due to incorrect checksum
    public int retransmissionCount;  // Number of retransmissions
    public int duplicateAckCount; // Number of duplicate acknowledgements

    private final static String FORMAT;
    private final static String HR;

    static {
        // Horizontal line
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
