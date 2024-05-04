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

public class Sender implements IClient {
    @SuppressWarnings("serial")
    private final static class WorkQueue extends ArrayBlockingQueue<DataSender> {
        public WorkQueue(int size) {
            super(size);
        }

        @Override
        public void put(DataSender ds) throws InterruptedException {
            super.put(ds);
            ds.send();
        }

        @Override
        public DataSender poll() {
            DataSender ds = super.poll();
            if (ds != null)
                ds.done();
            return ds;
        }
    }

    private final static class DataSender {
        private final Sender sender;
        private final PeriodicTask task;
        private final TCPPacket packet;
        private final DatagramPacket dPacket;
        private final int expectedAck;
        private int nRetries;

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
            nRetries = 0;
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
                if (nRetries > Sender.MAX_RETRIES)
                    done();
                System.out.printf(FORMAT,
                    "snd",
                    (System.nanoTime() - sender.startTime) / 1e6,
                    packet.isSyn() ? "S" : "-",
                    packet.isAck() ? "A" : "-",
                    packet.isFin() ? "F" : "-",
                    packet.getPayload().length > 0 ? "D" : "-",
                    packet.getSequenceNumber(),
                    packet.getPayload().length,
                    packet.getAcknowledgement()
                );
                sender.socket.send(dPacket);
                nRetries++;
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
                System.out.printf(FORMAT,
                    "rcv",
                    (System.nanoTime() - sender.startTime) / 1e6,
                    ackPacket.isSyn() ? "S" : "-",
                    ackPacket.isAck() ? "A" : "-",
                    ackPacket.isFin() ? "F" : "-",
                    ackPacket.getPayload().length > 0 ? "D" : "-",
                    ackPacket.getSequenceNumber(),
                    ackPacket.getPayload().length,
                    ackPacket.getAcknowledgement()
                );

                if (ackPacket.isFin()) {
                    stop();
                    synchronized (sender.monitor) {
                        sender.monitor.notify();
                    }
                    continue;
                }

                if (!ackPacket.isAck()) {
                    System.out.println("Not an Ack Packet!");
                    System.out.println("Wait actually what is this packet");
                    System.out.println("Discarding...");
                    continue;
                }

                int ack = ackPacket.getAcknowledgement();
                if (ack > sender.maxAck) {
                    synchronized (sender.workQueue) {
                        while (!sender.workQueue.isEmpty())
                            sender.workQueue.poll();
                        stop();
                        synchronized (sender.monitor) {
                            sender.monitor.notify();
                        }
                        return;
                    }
                }
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

                    while (top != null && ack >= top.expectedAck()) {
                        // It really shouldn't be possible for this to be null
                        // since this is synchronized
                        if (sender.doneSending) {
                            synchronized (sender.monitor) {
                                sender.monitor.notify();
                                continue;
                            }
                        }

                        top = sender.workQueue.poll();
                        top = sender.workQueue.peek();
                    }
                    if (ctr >= 3) {
                        top.fastRetransmit();
                        ctr = 0;
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
    private static final String FORMAT = "%s %.2f %s %s %s %s %d %d %d\n";

    private final SendConfig config;
    private final DatagramSocket socket;
    private final SocketAddress serverAddr;
    private final AckListener ackListener;
    private final Thread ackListenerThread;

    private double startTime;
    // Re-Transmission Timeout
    private volatile long timeout;
    private volatile long estimatedRoundTripTime;
    private volatile long estimatedDeviation;

    private volatile int seqNo;
    private volatile int lastSeqNo;
    private volatile boolean doneSending;
    private long maxAck;

    private final BlockingQueue<DataSender> workQueue;

    private boolean isConnected;

    // For synchronization
    private Object monitor;

    public Sender(SendConfig config) throws IOException {
        this.config = config;
        socket = new DatagramSocket(config.port());
        serverAddr = new InetSocketAddress(config.remoteIP(),
                                           config.remotePort());

        ackListener = new AckListener(this);
        ackListenerThread = new Thread(ackListener);

        workQueue = new WorkQueue(config.sws());
        isConnected = false;
        timeout = INITIAL_TIMEOUT;

        monitor = new Object();

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
        seqNo = 0;

        // Try upto MAX_RETRIES times
        for (int i = 0; i < MAX_RETRIES; i++) {
            final TCPPacket packet = new TCPPacket(seqNo, 0);
            packet.setFlag(TCPFlag.SYN, true);

            byte[] buf = packet.serialize();
            int len = buf.length;

            DatagramPacket pkt;
            pkt = new DatagramPacket(buf, len, serverAddr);
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
                continue;
            }

            lastSeqNo = recvPkt.getSequenceNumber();

            // Syn stays the same for an ACK packet
            TCPPacket ackPacket = new TCPPacket(seqNo, ++lastSeqNo);
            ackPacket.setFlag(TCPFlag.ACK, true);
            buf = ackPacket.serialize();
            len = buf.length;
            pkt = new DatagramPacket(buf, len, serverAddr);

            socket.send(pkt);  // Part 3 of 3-way handshake

            estimatedRoundTripTime = System.nanoTime() - recvPkt.getTimeStamp();
            estimatedDeviation = 0;

            timeout = 2 * estimatedRoundTripTime;

            isConnected = true;
            socket.setSoTimeout(0);
            seqNo++;
            startTime = System.nanoTime();
            return;
        }

        throw new IllegalStateException("Failed to connect!");
    }

    private void handleSynAck() {

    }

    public void sendFile() throws FileNotFoundException {
        send(config.fileName());
    }

    public void send(String filename) throws FileNotFoundException {
        send(new File(filename));
    }

    public void send(File file) throws FileNotFoundException {
        if (!isConnected)
            throw new IllegalStateException("Not connected!");
        doneSending = false;
        ackListener.start();
        ackListenerThread.start();

        final long fileLen = file.length();
        maxAck = fileLen;

        final int fileChunkSize = config.mtu() - TCPPacket.HEADER_SIZE;

        FileInputStream reader = new FileInputStream(file);

        for (int i = 0; i < fileLen; i += fileChunkSize) {
            int bufSize = (int) Math.min(fileChunkSize, fileLen - i);
            byte[] buf = new byte[bufSize];
            TCPPacket dataPacket = new TCPPacket(seqNo, lastSeqNo);

            try {
                reader.read(buf);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

            dataPacket.setPayload(buf);
            DataSender sender = new DataSender(this, dataPacket);

            try {
                workQueue.put(sender);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
            seqNo += fileChunkSize;
        }

        do {} while (workQueue.size() != 0);
        doneSending = true;
    }

    public void close() throws IOException {
        TCPPacket pkt = new TCPPacket(seqNo++, lastSeqNo);
        pkt.setFlag(TCPFlag.FIN, true);

        DataSender sender = new DataSender(this, pkt);
        synchronized (monitor) {
            sender.send();
        }
        boolean gotFinAck = false;
        for (int i = 0; i < MAX_RETRIES && !gotFinAck; i++) {
            byte[] buf = new byte[config.mtu()];
            DatagramPacket finAckDpkt = new DatagramPacket(buf, buf.length);

            socket.receive(finAckDpkt);
            pkt = (TCPPacket) pkt.deserialize(buf);
            System.out.printf(FORMAT,
                "rcv",
                (System.nanoTime() - startTime) / 1e6,
                pkt.isSyn() ? "S" : "-",
                pkt.isAck() ? "A" : "-",
                pkt.isFin() ? "F" : "-",
                pkt.getPayload().length > 0 ? "D" : "-",
                pkt.getSequenceNumber(),
                pkt.getPayload().length,
                pkt.getAcknowledgement()
            );

            if (pkt.isAck()) {
                System.out.println("GOT FIN ACK");
                gotFinAck = true;
                sender.done();
            }

            if (pkt.isFin()) {
                pkt.setFlag(TCPFlag.FIN, false);
                pkt.setFlag(TCPFlag.ACK, true);
                sender.fastRetransmit(); // Allows us to send non-periodic
            }
        }
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
