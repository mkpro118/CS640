import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Recv implements IServer {
    private final RecvConfig config;
    private DatagramSocket socket;
    private SocketAddress serverAddr;

    private final BlockingQueue<TCPPacket> workQueue;
    private final Thread worker;
    private final Metrics metrics;

    private int seqNo;
    private int nextByte;
    private boolean isConnected;
    private double startTime;
    private volatile long timeout;
    private volatile long estimatedRoundTripTime;
    private volatile long estimatedDeviation;

    private static final int MAX_RETRIES = 0x10;
    private static final int INITIAL_TIMEOUT = 0x1388;
    private static final String FORMAT = "%s %.2f %s %s %s %s %d %d %d\n";

    public Recv(RecvConfig config) {
        this.config = config;
        seqNo = 0;
        timeout = INITIAL_TIMEOUT;
        workQueue = new ArrayBlockingQueue<>(config.sws());
        worker = new Thread(this::writeToFile);
        metrics = new Metrics();

        // Ensure connection closes in case of an unexpected error
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (socket != null && !socket.isClosed())
                    socket.close();
                System.out.println(metrics);
            }
        });
    }

    @Override
    public void bind() throws IOException {
        socket = new DatagramSocket(config.port());
    }

    @Override
    public void accept() throws IOException {
        byte[] buf = new byte[config.mtu()];
        DatagramPacket packet;
        TCPPacket synPacket = new TCPPacket();

        // Get SYN
        while (true) {
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            serverAddr = packet.getSocketAddress();

            synPacket = (TCPPacket) synPacket.deserialize(buf);
            log("rcv", synPacket);

            if (synPacket.isSyn())
                break;
        }

        // Send SYN + ACK
        nextByte = synPacket.getSequenceNumber() + 1;

        for (int i = 0; i < MAX_RETRIES; i++) {
            TCPPacket synAckPacket = new TCPPacket(seqNo, nextByte);
            synAckPacket.setFlag(TCPFlag.SYN, true);
            synAckPacket.setFlag(TCPFlag.ACK, true);

            buf = synAckPacket.serialize();
            packet = new DatagramPacket(buf, buf.length, serverAddr);

            log("snd", synAckPacket);
            socket.send(packet);

            // GET ACK
            socket.setSoTimeout(INITIAL_TIMEOUT);

            try {
                buf = new byte[config.mtu()];
                DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
                socket.receive(ackPacket);
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                continue;
            }

            TCPPacket recvAckPacket = new TCPPacket();
            recvAckPacket = (TCPPacket) recvAckPacket.deserialize(buf);

            log("rcv", recvAckPacket);

            if (recvAckPacket.isSyn()) {
                nextByte = synPacket.getSequenceNumber() + 1;
                continue;
            }

            if (!recvAckPacket.isAck())
                continue;

            if (recvAckPacket.getAcknowledgement() == 1) {
                estimatedRoundTripTime = System.nanoTime() - recvAckPacket.getTimeStamp();
                estimatedDeviation = 0;

                timeout = 2 * estimatedRoundTripTime;

                isConnected = true;
                socket.setSoTimeout(0);
                seqNo++;
                startTime = System.nanoTime();
                return;
            }
        }

        throw new IllegalStateException("Failed to accept");
    }

    @Override
    public void start() throws IOException {
        worker.start();
        byte[] buf;
        DatagramPacket pkt;
        while (isConnected) {
            buf = new byte[config.mtu()];
            pkt = new DatagramPacket(buf, buf.length);

            socket.receive(pkt);

            TCPPacket dataPacket = new TCPPacket();
            dataPacket = (TCPPacket) dataPacket.deserialize(buf);
            log("rcv", dataPacket);

            if (dataPacket.isFin()) {
                isConnected = false;
                TCPPacket ackPacket = new TCPPacket(seqNo, nextByte);
                ackPacket.setFlag(TCPFlag.ACK, true);
                buf = ackPacket.serialize();
                pkt = new DatagramPacket(buf, buf.length, serverAddr);
                log("snd", ackPacket);
                socket.send(pkt);
                return;
            }

            if (dataPacket.isSyn() || dataPacket.isAck()) {
                System.out.println("Unexpected SYN/ACK packet");
                System.out.println(dataPacket);
                continue;
            }

            if (dataPacket.getSequenceNumber() == nextByte && workQueue.offer(dataPacket))
                nextByte += dataPacket.getPayload().length;

            TCPPacket ackPacket = new TCPPacket(seqNo, nextByte);
            ackPacket.setFlag(TCPFlag.ACK, true);
            buf = ackPacket.serialize();
            pkt = new DatagramPacket(buf, buf.length, serverAddr);
            log("snd", ackPacket);
            socket.send(pkt);
        }
    }

    private void writeToFile() {
        FileOutputStream writer = null;
        try {
            writer = new FileOutputStream(config.fileName());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while (isConnected) {
            TCPPacket packet = null;
            try {
                packet = workQueue.take();
            } catch (InterruptedException e) {
                if (isConnected) {
                    e.printStackTrace();
                    System.exit(1);
                }
                return;
            }
            byte[] buf = packet.getPayload();
            try {
                writer.write(buf);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    @Override
    public void close() throws IOException {
        isConnected = false;
        worker.interrupt();

        (new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(INITIAL_TIMEOUT);
                    System.exit(0); // CLOSE
                } catch (InterruptedException e) {}
            }
        }).start();

        TCPPacket pkt = new TCPPacket(seqNo++, nextByte);
        pkt.setFlag(TCPFlag.FIN, true);
        byte[] finBuf = pkt.serialize();
        DatagramPacket dPkt = new DatagramPacket(finBuf, finBuf.length, serverAddr);

        PeriodicTask finSend = new PeriodicTask(
            () -> {
                try {
                    socket.send(dPkt);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(-11);
                }
            }, timeout
        );

        finSend.start();

        (new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(INITIAL_TIMEOUT);
                    System.exit(0); // CLOSE
                } catch (InterruptedException e) {}
            }
        }).start();

        for (int i = 0; i < MAX_RETRIES; i++) {
            byte[] buf = new byte[config.mtu()];
            DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(recvPacket);
            } catch (SocketException e) {
                // Socket was closed by timeout
                System.exit(0);
            }

            TCPPacket recvPkt = new TCPPacket();
            recvPkt = (TCPPacket) recvPkt.deserialize(buf);
            log("rcv", recvPkt);

            if (recvPkt.isFin()) {
                TCPPacket ack;
                ack = new TCPPacket(seqNo, recvPkt.getSequenceNumber() + 1);
                ack.setFlag(TCPFlag.ACK, true);
                buf = ack.serialize();
                recvPacket = new DatagramPacket(buf, buf.length, serverAddr);
                log("snd", ack);
                socket.send(recvPacket);
                continue;
            } else if (recvPkt.isAck()) {
                finSend.stop();
            }
        }
    }

    private final void log(String type, TCPPacket packet) {
        System.out.printf(FORMAT,
            type,
            (System.nanoTime() - startTime) / 1e6,
            packet.isSyn() ? "S" : "-",
            packet.isAck() ? "A" : "-",
            packet.isFin() ? "F" : "-",
            packet.getPayload().length > 0 ? "D" : "-",
            packet.getSequenceNumber(),
            packet.getPayload().length,
            packet.getAcknowledgement()
        );
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
