import java.nio.ByteBuffer;

public class Utils {
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

    public static byte[] longToBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip(); //need flip
        return buffer.getLong();
    }

    public static int setFlag_S(int x, boolean set);
    public static int setFlag_F(int x, boolean set);
    public static int setFlag_A(int x, boolean set);
    public static int getFlag_S(int x, boolean set);
    public static int getFlag_F(int x, boolean set);
    public static int getFlag_A(int x, boolean set);
}
