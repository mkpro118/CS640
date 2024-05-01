import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;


public class Sender {
    private final static class AckListener implements Runnable {
        private final Sender sender;
        private final byte[] buf;
        private boolean active;

        public AckListener(final Sender sender) {
            this.sender = sender;
            buf = new byte[sender.config.mtu()];
        }

        @Override
        public void run() {
            DatagramPacket pkt;
            while (active) {
                Arrays.fill(buf, (byte) 0); // Reset the buffer

                pkt = new DatagramPacket(buf, sender.config.mtu());

                try {
                    sender.socket.receive(pkt);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

        public void start() {
            active = true;
        }

        public void stop() {
            active = false;
        }
    }

    private static final int MAX_RETRIES = 0x10;
    private static final int INITIAL_TIMEOUT = 0x1388;

    private final SendConfig config;
    private final DatagramSocket socket;
    private final SocketAddress serverAddr;

    private final TCPPacket[] workQueue;
    private volatile int workQueueIdx;

    private int ackIdx;
    private boolean isConnected;

    public Sender(SendConfig config) throws IOException {
        this.config = config;
        socket = new DatagramSocket(config.port());
        serverAddr = new InetSocketAddress(config.remoteIP(),
                                           config.remotePort());
        ackIdx = 0;
        isConnected = false;
        workQueue = new TCPPacket[config.sws()];
        workQueueIdx = 0;
    }

    public void connect() throws IOException {
        TCPPacket packet = new TCPPacket(0, 0);
        packet.setFlag(TCPFlag.SYN, true);
        isConnected = true;
    }

    public void send(String filename) throws FileNotFoundException {
        send(new File(filename));
    }

    public void send(File file) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
    }

    private void incr() {
        ackIdx = (ackIdx + 1) % workQueue.length;
    }

    private static void sendPacket(SocketAddress addr, TCPPacket packet) {
        byte[] pkt = packet.serialize();
        int len = pkt.length;

        DatagramPacket dPacket = new DatagramPacket(pkt, len, addr);

    }

}
