import java.util.Arrays;

public class TCPPacket implements ITCPPacket {
    private int sequenceNumber;
    private int acknowledgement;
    private long timeStamp;
    private int length;
    private short checksum;
    private byte[] payload;
    private short internalChecksum;

    // Maximum segment size
    private static int MSS;

    // Size of the TCP Header
    public final static int HEADER_SIZE;

    // 2 bytes of all zeros as used in the given TCP Header
    private final static byte[] ALL_ZEROS;

    // Sizes of Header fields in bytes
    private final static int SEQUENCE_NUMBER_SIZE;
    private final static int ACKNOWLEDGEMENT_SIZE;
    private final static int TIMESTAMP_SIZE;
    private final static int LENGTH_SIZE;
    private final static int ALL_ZEROS_SIZE;
    private final static int CHECKSUM_SIZE;
    private final static int CHECKSUM_OFFSET;

    // Length shift in header
    private final static int LENGTH_SHIFT;

    // Mask to extract bytes
    private final static long BYTE_MASK;

    // Flags mask
    private final static int FLAGS_MASK;

    // Mask to extract the 17th bit for checksum computation
    private final static int BIT_17_MASK = 0x10000;

    // Shift to add the carry over 17th bit
    private final static int BIT_17_SHIFT = 0x10;

    static {
        MSS = 0x0;
        HEADER_SIZE = 0x18;
        ALL_ZEROS = new byte[]{0x0, 0x0};
        BYTE_MASK = 0xFFL;
        SEQUENCE_NUMBER_SIZE = 0x4;
        ACKNOWLEDGEMENT_SIZE = 0x4;
        TIMESTAMP_SIZE = 0x8;
        LENGTH_SIZE = 0x4;
        ALL_ZEROS_SIZE = 0x2;
        CHECKSUM_SIZE = 0x2;
        FLAGS_MASK = TCPFlag.SYN.mask | TCPFlag.FIN.mask | TCPFlag.ACK.mask;
        LENGTH_SHIFT = 3;
        CHECKSUM_OFFSET = (SEQUENCE_NUMBER_SIZE + ACKNOWLEDGEMENT_SIZE
                               + TIMESTAMP_SIZE + LENGTH_SIZE + ALL_ZEROS_SIZE);
    }

    public static void setMTU(int mtu) {
        if (mtu <= HEADER_SIZE)
            throw new IllegalArgumentException(
                "Need MSS to at least contain the TCP Header and 1 byte of data"
            );
        MSS = mtu - HEADER_SIZE;
    }

    public TCPPacket() {}

    public TCPPacket(final int seqNo, final int ack) {
        if (MSS == 0)
            throw new IllegalStateException("Set MSS before creating packets");

        sequenceNumber = seqNo;
        acknowledgement = ack;
        payload = new byte[0];
        length = 0;
        checksum = 0;
        internalChecksum = 0;
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
        if (payload.length > MSS)
            throw new AssertionError(
                "Payload too large. MSS = " + MSS
              + ". Payload size = " + payload.length
              + ". Need to reserve at least " + HEADER_SIZE
              + " bytes for TCP Header.");
        this.payload = payload;
        length &= FLAGS_MASK;
        length |= payload.length << LENGTH_SHIFT;
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
            toBytes(checksum),  // 2 bytes (checksum is zero before computation)
        };

        // Write out the header.
        for (byte[] item: header) {
            System.arraycopy(item, 0, packet, pos, item.length);
            pos += item.length;
        }

        System.arraycopy(payload, 0, packet, pos, payload.length);

        if (checksum == 0 || checksum != internalChecksum)
            internalChecksum = checksum = computeChecksum(packet);


        return packet;
    }

    @Override
    public ITCPPacket deserialize(byte[] packet) {
        int pos = 0;

        sequenceNumber = intFromBytes(packet, pos);

        pos += SEQUENCE_NUMBER_SIZE; // Move to ack
        acknowledgement = intFromBytes(packet, pos);

        pos += ACKNOWLEDGEMENT_SIZE; // Move to timestamp
        timeStamp = longFromBytes(packet, pos);

        pos += TIMESTAMP_SIZE; // Move to length
        length = intFromBytes(packet, pos);

        pos += LENGTH_SIZE + ALL_ZEROS_SIZE; // Move to checksum
        checksum = shortFromBytes(packet, pos);

        pos += CHECKSUM_SIZE; // Move to Payload
        if (packet.length - pos < length >> LENGTH_SHIFT)
            throw new IllegalStateException("packet.length " + packet.length + " pos " + pos + " length >> LENGTH_SHIFT " + (length >> LENGTH_SHIFT));

        payload = new byte[length >> LENGTH_SHIFT];
        System.arraycopy(packet, pos, payload, 0, payload.length);

        return this;
    }

    @Override
    public short getChecksum() {
        return checksum;
    }

    @Override
    public void setChecksum(short checksum) {
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        return String.format(
            "TCP(seqNo=%d, ack=%d, timestamp=%d, length=%d, flags=%s%s%s)",
            sequenceNumber,
            acknowledgement,
            timeStamp,
            length /*>> 3*/,
            isSyn() ? "S" : "_",
            isFin() ? "F" : "_",
            isAck() ? "A" : "_"
        );
    }

    private final static long longFromBytes(byte[] val, int pos, int nBytes) {
        long res = 0;
        int end = pos + nBytes;
        for (int i = pos; i < end; i++)
            res = (res << 8) + (val[i] & BYTE_MASK);
        return res;
    }

    private final static short shortFromBytes(byte[] val, int pos) {
        return (short) longFromBytes(val, pos, 2);
    }

    private final static int intFromBytes(byte[] val, int pos) {
        return (int) longFromBytes(val, pos, 4);
    }

    private final static long longFromBytes(byte[] val, int pos) {
        return longFromBytes(val, pos, 8);
    }

    private final static byte[] toBytes(long val, int nBytes) {
        final byte[] arr = new byte[nBytes];

        for (int i = 0; i < arr.length; i++) {
            int shift = (8 * (nBytes - i - 1));
            arr[i] = (byte) ((val & (BYTE_MASK << shift)) >> shift);
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

    private static final short checksumAdd(short x, short y) {
        int r = x + y;
        return (short) (r + ((r & BIT_17_MASK) >> BIT_17_SHIFT));
    }

    private static final short computeChecksum(byte[] packet) {
        // Forcefully set checksum to zero
        System.arraycopy(ALL_ZEROS, 0, packet, CHECKSUM_OFFSET, ALL_ZEROS_SIZE);

        short checksum = shortFromBytes(packet, 0);

        int len = packet.length & (~1);

        // For every 2 bytes
        for (int i = 2; i < len; i += 2) {
            checksum = checksumAdd(checksum, shortFromBytes(packet, i));
        }

        // Add extra NUL byte for padding
        if ((packet.length & 1) == 1) {
            short segment = shortFromBytes(new byte[]{packet[len], 0x0}, 0);
            checksum = checksumAdd(checksum, segment);
        }

        return (short)(~checksum);
    }
}
