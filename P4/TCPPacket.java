import java.util.Arrays;

public class TCPPacket implements ITCPPacket {
    private final int sequenceNumber;
    private final int acknowledgement;
    private long timeStamp;
    private int length;
    private short checksum;
    private byte[] payload;


    // Maximum segment size
    private static int MSS;

    // Size of the TCP Header
    private final static int HEADER_SIZE;

    // 2 bytes of all zeros as used in the given TCP Header
    private final static byte[] ALL_ZEROS;

    // Mask to extract bytes
    private final static long MASK;

    static {
        MSS = 0;
        HEADER_SIZE = 24;
        ALL_ZEROS = new byte[]{0, 0};
        MASK = 0xFFL;
    }

    public TCPPacket(final int seqNo, final int ack) {
        if (MSS == 0)
            throw new IllegalStateException("Set MSS before creating packets");

        sequenceNumber = seqNo;
        acknowledgement = ack;
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

    public boolean isSyn() { return (length & TCPFlag.SYN.mask) != 0; }

    public boolean isAck() { return (length & TCPFlag.ACK.mask) != 0; }

    public boolean isFin() { return (length & TCPFlag.FIN.mask) != 0; }

    public byte[] serialize() {
        int pos = 0;

        byte[] packet = new byte[HEADER_SIZE + payload.length];

        byte[][] header = {
            toBytes(sequenceNumber),  // 4 bytes
            toBytes(acknowledgement),  // 4 bytes
            toBytes(timeStamp = System.nanoTime()),  // 8 bytes
            toBytes(length),  // 4 bytes, contains SFA flags
            ALL_ZEROS,  // 2 bytes
            toBytes(checksum()),  // 2 bytes
        };

        // Write out the header.
        for (byte[] item: header) {
            System.arraycopy(item, 0, packet, pos, item.length);
            pos += item.length;
        }

        System.arraycopy(payload, 0, packet, pos, payload.length);

        return packet;
    }

    public ITCPPacket deserialize(byte[] packet) {

        return null;
    }

    public short checksum() {
        return 0;
    }

    private final static byte[] toBytes(long val, int retSize) {
        final byte[] arr = new byte[retSize];

        for (int i = 0; i < arr.length; i++) {
            int shift = (8 * (retSize - i - 1));
            arr[i] = (byte) ((val & (MASK << shift)) >> shift);
        }

        return arr;
    }

    private final static byte[] toBytes(short val) { return toBytes(val, 2); }

    private final static byte[] toBytes(int val) { return toBytes(val, 4); }

    private final static byte[] toBytes(long val) { return toBytes(val, 8); }

    public static void main(String[] args) {
        TCPPacket.setMSS(1500);
        TCPPacket packet = new TCPPacket(0, 0);
        packet.setPayload("Hello World!".getBytes());

        byte[] bytes = packet.serialize();
        for (int i = 0; i < bytes.length; i += 4) {
            byte[] word = new byte[4];
            System.arraycopy(bytes, i, word, 0, Math.min(bytes.length - i, word.length));
            System.out.println(Arrays.toString(word));
        }
    }
}
