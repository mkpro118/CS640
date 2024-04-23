import java.util.Arrays;

public class TCPPacket implements ITCPPacket {
    private final int sequenceNumber;
    private final int acknowledgement;
    private long timeStamp;
    private int length;
    private short checksum;
    private byte[] buffer;
    private byte[] payload;


    // Maximum segment size
    private static int MSS;

    public final static int HEADER_SIZE;

    static {
        MSS = 0;
        HEADER_SIZE = 24;
    }

    public TCPPacket(final int seqNo, final int ack) {
        if (MSS == 0)
            throw new IllegalStateException("Set MSS before creating packets");

        sequenceNumber = seqNo;
        acknowledgement = ack;
        buffer = new byte[MSS];
        payload = new byte[0];
        length = 0;
    }

    public int getSequenceNumber() { return sequenceNumber; }

    public int getAcknowledgement() { return acknowledgement; }

    public long getTimeStamp() { return timeStamp; }

    public static void setMSS(int mss) {
        if (mss <= HEADER_SIZE)
            throw new IllegalArgumentException(
                "Need MSS to at least contain the TCP Header and 1 byte of data"
            );
        MSS = mss;
    }

    public void setPayload(byte[] payload) {
        if (payload.length + HEADER_SIZE > MSS)
            throw new AssertionError(
                "Payload too large. MSS = " + MSS
              + ". Payload size = " + payload.length
              + ". Need to reserve at least " + HEADER_SIZE
              + " bytes for TCP Header.");
        this.payload = payload;
        length &= 0b111;
        length |= payload.length << 3;
    }

    public byte[] getPayload() { return this.payload; }

    public void setFlag(TCPFlag flag, boolean on) {
        if (on)
            length |= flag.mask;
        else
            length &= ~flag.mask;
    }

    public boolean isSyn() {
        return (length & TCPFlag.SYN.mask) != 0;
    }

    public boolean isAck() {
        return (length & TCPFlag.ACK.mask) != 0;
    }

    public boolean isFin() {
        return (length & TCPFlag.FIN.mask) != 0;
    }

    public byte[] serialize() {
        Arrays.fill(buffer, (byte) 0);
        byte[] packet = new byte[HEADER_SIZE + payload.length];
        return null;
    }

    public ITCPPacket deserialize(byte[] packet) {
        return null;
    }
    public short checksum() {
        return 0;
    }
}
