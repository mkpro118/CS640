import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Sender {
    private final static class DataSender {
        private final Sender sender;
        private final PeriodicTask task;
        private final TCPPacket packet;
        private final DatagramPacket dPacket;
        private final int expectedAck;

        public DataSender(Sender sender, TCPPacket packet) {
            this.sender = sender;
            this.packet = packet;
            expectedAck = packet.getSequenceNumber() + packet.getPayload().length;

            byte[] pkt = packet.serialize();
            int len = pkt.length;

            if (len > sender.config.mtu())
                throw new IllegalStateException(
                    "Packet length is longer than MTU " + len + " > "
                    + sender.config.mtu());

            dPacket = new DatagramPacket(pkt, len, sender.serverAddr);

            task = new PeriodicTask(this::sendPacket, sender.timeout);
        }

        public int expectedAck() {
            return expectedAck;
        }

        public long timeStamp() {
            return packet.getTimeStamp();
        }

        public void send() {
            task.start();
        }

        public void done() {
            task.stop();
        }

        public void fastRetransmit() {
            sendPacket();
        }

        private void sendPacket() {
            try {
                sender.socket.send(dPacket);
            } catch (IOException e) {
                // Something went wrong!
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

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
            int ctr = 0;
            int lastAck = 0;
            while (active) {
                Arrays.fill(buf, (byte) 0); // Reset the buffer

                pkt = new DatagramPacket(buf, sender.config.mtu());

                try {
                    sender.socket.receive(pkt);
                } catch (IOException e) {
                    if (sender.socket.isClosed())
                        return;

                    // Something went wrong!
                    e.printStackTrace();
                    System.exit(1);
                }

                TCPPacket ackPacket = (new TCPPacket());
                ackPacket = (TCPPacket) ackPacket.deserialize(buf);

                if (!ackPacket.isAck()) {
                    System.out.println("Not an Ack Packet!");
                    continue;
                }

                int ack = ackPacket.getAcknowledgement();
                if (ack == lastAck) {
                    ctr++;
                } else {
                    ctr = 0;
                    lastAck = ack;
                }

                synchronized (sender.workQueue) {
                    DataSender top = sender.workQueue.peek();
                    // There's nothing in the queue, but technically we
                    // shouldn't get an ack here then, unless the ack somehow
                    // got delayed, and we retransmitted
                    if (top == null)
                        continue;

                    if (ack == top.expectedAck()) {
                        // It really shouldn't be possible for this to be null
                        // since this is synchronized
                        top = sender.workQueue.poll();
                        top.done();
                    } else if (ctr >= 3) {
                        top.fastRetransmit();
                        continue;
                    }
                }

                long ackTimestamp = ackPacket.getTimeStamp();

                sender.recomputeTimeout(ackTimestamp);
            }
        }

        public void start() { active = true; }

        public void stop() { active = false; }
    }

    private static final int MAX_RETRIES = 0x10;
    private static final int INITIAL_TIMEOUT = 0x1388;

    private final SendConfig config;
    private final DatagramSocket socket;
    private final SocketAddress serverAddr;

    // Re-Transmission Timeout
    private volatile long timeout;
    private volatile long estimatedRoundTripTime;
    private volatile long estimatedDeviation;

    private volatile int syn;
    private volatile int lastSeqNo;

    private final BlockingQueue<DataSender> workQueue;

    private boolean isConnected;

    public Sender(SendConfig config) throws IOException {
        this.config = config;
        socket = new DatagramSocket(config.port());
        serverAddr = new InetSocketAddress(config.remoteIP(),
                                           config.remotePort());
        workQueue = new ArrayBlockingQueue<>(config.sws());
        isConnected = false;
        timeout = INITIAL_TIMEOUT;

        // Ensure connection closes in case of an unexpected error
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                if (!socket.isClosed())
                    socket.close();
            }
        });
    }

    public void connect() throws IOException {
        // Sequence number is 0 on SYN, ACK doesn't matter but we set it to 0.
        syn = 0;
        final TCPPacket packet = new TCPPacket(syn, 0);
        packet.setFlag(TCPFlag.SYN, true);

        byte[] buf = packet.serialize();
        int len = buf.length;

        DatagramPacket pkt = new DatagramPacket(buf, len, serverAddr);

        // Try upto MAX_RETRIES times
        for (int i = 0; i < MAX_RETRIES; i++) {
            socket.send(pkt);  // Part 1 of 3-way handshake

            buf = new byte[config.mtu()];
            len = buf.length;
            pkt = new DatagramPacket(buf, len);

            socket.setSoTimeout(INITIAL_TIMEOUT);
            try {
                socket.receive(pkt);  // Part 2 of 3-way handshake
            } catch (SocketTimeoutException e) {
                continue;
            }

            TCPPacket recvPkt = (TCPPacket)(new TCPPacket()).deserialize(buf);

            if (!recvPkt.isSyn() || !recvPkt.isAck()) {
                System.out.println("Wrong packet!");
                continue;
            }

            lastSeqNo = recvPkt.getSequenceNumber();

            // Syn stays the same for an ACK packet
            TCPPacket ackPacket = new TCPPacket(syn, lastSeqNo + 1);
            ackPacket.setFlag(TCPFlag.ACK, true);
            buf = packet.serialize();
            len = buf.length;
            pkt = new DatagramPacket(buf, len, serverAddr);
            socket.send(pkt);  // Part 3 of 3-way handshake

            estimatedRoundTripTime = System.nanoTime() - recvPkt.getTimeStamp();
            estimatedDeviation = 0;

            timeout = 2 * estimatedRoundTripTime;

            isConnected = true;
            socket.setSoTimeout(0);
            return;
        }

        throw new IllegalStateException("Failed to connect!");
    }

    public void send(String filename) throws FileNotFoundException {
        send(new File(filename));
    }

    public void send(File file) throws FileNotFoundException {
        if (!isConnected)
            throw new IllegalStateException("Not connected!");

        final int fileLen = file.length();

        final int fileChunkSize = config.mtu() - TCPPacket.HEADER_SIZE;

        FileInputStream reader = new FileInputStream(file);


    }

    private void recomputeTimeout(long ackTimeStamp) {
        final double a = 0.875;
        final double b = 0.75;

        final long C = System.nanoTime();
        final long T = ackTimeStamp;

        long ERTT = estimatedRoundTripTime;
        long EDEV = estimatedDeviation;

        final long SRTT = (C - T);
        final long SDEV = Math.abs(SRTT - ERTT);
        ERTT = (long)(a * ERTT + (1 - a) * SRTT);
        EDEV = (long)(b * EDEV + (1 - b) * SDEV);
        final long TO = ERTT + 4 * EDEV;

        estimatedRoundTripTime = ERTT;
        estimatedDeviation = EDEV;
        timeout = TO;
    }
}
