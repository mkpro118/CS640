/**
 * The flags in TCP packet header
 */
public enum TCPFlag {
    SYN(0b100),
    FIN(0b10),
    ACK(0b1);

    public final int mask;
    private TCPFlag(int mask) {
        this.mask = mask;
    }
}
