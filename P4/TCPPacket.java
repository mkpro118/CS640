import java.util.Arrays;

public class TCPPacket implements ITCPPacket {
    private int sequenceNumber;
    private int acknowledgement;
    private long timeStamp;
    private int length;
    private short checksum;
    private byte[] payload;
    private boolean init;

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

    public static void setMSS(int mss) {
        if (mss <= HEADER_SIZE)
            throw new IllegalArgumentException(
                "Need MSS to at least contain the TCP Header and 1 byte of data"
            );
        MSS = mss;
    }

    public TCPPacket() {
        init = false;
    }

    public TCPPacket(final int seqNo, final int ack) {
        if (MSS == 0)
            throw new IllegalStateException("Set MSS before creating packets");

        sequenceNumber = seqNo;
        acknowledgement = ack;
        payload = new byte[0];
        length = 0;
        init = true;
    }

    @Override
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public int getAcknowledgement() {
        return acknowledgement;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
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

    @Override
    public byte[] getPayload() {
        return this.payload;
    }

    @Override
    public void setFlag(TCPFlag flag, boolean on) {
        if (on)
            length |= flag.mask;
        else
            length &= ~flag.mask;
    }

    @Override
    public boolean isSyn() {
        return (length & TCPFlag.SYN.mask) != 0;
    }

    @Override
    public boolean isAck() {
        return (length & TCPFlag.ACK.mask) != 0;
    }

    @Override
    public boolean isFin() {
        return (length & TCPFlag.FIN.mask) != 0;
    }

    @Override
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

    @Override
    public ITCPPacket deserialize(byte[] packet) {
        byte[] buf = new byte[8];
        int pos = 0;

        System.arraycopy(packet, pos, buf, 0, 4);
        sequenceNumber = intFromBytes(buf);

        pos += 4; // Move to ack
        System.arraycopy(packet, pos, buf, 0, 4);
        acknowledgement = intFromBytes(buf);

        pos += 4; // Move to timestamp
        System.arraycopy(packet, pos, buf, 0, 8);
        timeStamp = longFromBytes(buf);

        pos += 8; // Move to length
        System.arraycopy(packet, pos, buf, 0, 4);
        length = intFromBytes(buf);

        pos += 6; // +4 for length, +2 for the `All Zeros`
        System.arraycopy(packet, pos, buf, 0, 2);
        checksum = shortFromBytes(buf);

        pos += 2; // Move to Payload
        if (packet.length - pos < length >> 3)
            throw new IllegalStateException();

        payload = new byte[length >> 3];
        System.arraycopy(packet, pos, payload, 0, payload.length);

        return this;
    }

    @Override
    public short checksum() {
        return 0;
    }

    @Override
    public String toString() {
        return String.format(
            "TCP(seqNo=%d, ack=%d, timestamp=%d, length=%d, flags=%s%s%s)",
            sequenceNumber,
            acknowledgement,
            timeStamp,
            length >> 3,
            isSyn() ? "S" : "_",
            isFin() ? "F" : "_",
            isAck() ? "A" : "_"
        );
    }

    private final static long longFromBytes(byte[] val, int nBytes) {
        long res = 0;
        for (int i = 0; i < nBytes; i++)
            res = (res << 8) + (val[i] & MASK);
        return res;
    }

    private final static short shortFromBytes(byte[] val) {
        return (short) longFromBytes(val, 2);
    }

    private final static int intFromBytes(byte[] val) {
        return (int) longFromBytes(val, 4);
    }

    private final static long longFromBytes(byte[] val) {
        return longFromBytes(val, 8);
    }

    private final static byte[] toBytes(long val, int nBytes) {
        final byte[] arr = new byte[nBytes];

        for (int i = 0; i < arr.length; i++) {
            int shift = (8 * (nBytes - i - 1));
            arr[i] = (byte) ((val & (MASK << shift)) >> shift);
        }

        return arr;
    }

    private final static byte[] toBytes(short val) {
        return toBytes(val, 2);
    }

    private final static byte[] toBytes(int val) {
        return toBytes(val, 4);
    }

    private final static byte[] toBytes(long val) {
        return toBytes(val, 8);
    }
}
