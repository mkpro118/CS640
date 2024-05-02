import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Recv implements IServer {
    private final RecvConfig config;
    private DatagramSocket socket;
    private SocketAddress serverAddr;

    private final BlockingQueue<TCPPacket> workQueue;
    private final Thread worker;

    private int seqNo;
    private int nextByte;
    private boolean isConnected;
    private double startTime;

    private static final int MAX_RETRIES = 0x10;
    private static final int INITIAL_TIMEOUT = 0x1388;
    private static final String FORMAT = "%s %.2f %s %s %s %s %d %d %d\n";

    public Recv(RecvConfig config) {
        this.config = config;
        seqNo = 0;
        workQueue = new ArrayBlockingQueue<>(config.sws());
        worker = new Thread(this::writeToFile);

        // Ensure connection closes in case of an unexpected error
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (socket != null && !socket.isClosed())
                    socket.close();
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
            System.out.printf(FORMAT,
                    "rcv",
                    (System.nanoTime() - startTime) / 1e6,
                    synPacket.isSyn() ? "S" : "-",
                    synPacket.isAck() ? "A" : "-",
                    synPacket.isFin() ? "F" : "-",
                    synPacket.getPayload().length > 0 ? "D" : "-",
                    synPacket.getSequenceNumber(),
                    synPacket.getPayload().length,
                    synPacket.getAcknowledgement()
                );
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

            System.out.printf(FORMAT,
                    "snd",
                    (System.nanoTime() - startTime) / 1e6,
                    synAckPacket.isSyn() ? "S" : "-",
                    synAckPacket.isAck() ? "A" : "-",
                    synAckPacket.isFin() ? "F" : "-",
                    synAckPacket.getPayload().length > 0 ? "D" : "-",
                    synAckPacket.getSequenceNumber(),
                    synAckPacket.getPayload().length,
                    synAckPacket.getAcknowledgement()
                );
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

            System.out.printf(FORMAT,
                    "rcv",
                    (System.nanoTime() - startTime) / 1e6,
                    recvAckPacket.isSyn() ? "S" : "-",
                    recvAckPacket.isAck() ? "A" : "-",
                    recvAckPacket.isFin() ? "F" : "-",
                    recvAckPacket.getPayload().length > 0 ? "D" : "-",
                    recvAckPacket.getSequenceNumber(),
                    recvAckPacket.getPayload().length,
                    recvAckPacket.getAcknowledgement()
                );

            if (recvAckPacket.isSyn()) {
                nextByte = synPacket.getSequenceNumber() + 1;
                continue;
            }

            if (!recvAckPacket.isAck())
                continue;

            if (recvAckPacket.getAcknowledgement() == 1) {
                socket.setSoTimeout(0);
                seqNo++;
                isConnected = true;
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
            System.out.printf(FORMAT,
                    "rcv",
                    (System.nanoTime() - startTime) / 1e6,
                    dataPacket.isSyn() ? "S" : "-",
                    dataPacket.isAck() ? "A" : "-",
                    dataPacket.isFin() ? "F" : "-",
                    dataPacket.getPayload().length > 0 ? "D" : "-",
                    dataPacket.getSequenceNumber(),
                    dataPacket.getPayload().length,
                    dataPacket.getAcknowledgement()
                );

            if (dataPacket.isFin()) {
                isConnected = false;
                TCPPacket ackPacket = new TCPPacket(seqNo, nextByte);
                ackPacket.setFlag(TCPFlag.ACK, true);
                buf = ackPacket.serialize();
                pkt = new DatagramPacket(buf, buf.length, serverAddr);
                System.out.printf(FORMAT,
                    "snd",
                    (System.nanoTime() - startTime) / 1e6,
                    ackPacket.isSyn() ? "S" : "-",
                    ackPacket.isAck() ? "A" : "-",
                    ackPacket.isFin() ? "F" : "-",
                    ackPacket.getPayload().length > 0 ? "D" : "-",
                    ackPacket.getSequenceNumber(),
                    ackPacket.getPayload().length,
                    ackPacket.getAcknowledgement()
                );
                socket.send(pkt);
                return;
            }

            if (dataPacket.isSyn() || dataPacket.isAck()) {
                System.out.println("Unexpected SYN/ACK packet");
                System.out.println(dataPacket);
                continue;
            }

            if (dataPacket.getSequenceNumber() != nextByte) {
                continue;
            }

            if (workQueue.offer(dataPacket))
                nextByte += dataPacket.getPayload().length;

            TCPPacket ackPacket = new TCPPacket(seqNo, nextByte);
            ackPacket.setFlag(TCPFlag.ACK, true);
            buf = ackPacket.serialize();
            pkt = new DatagramPacket(buf, buf.length, serverAddr);
            System.out.printf(FORMAT,
                "snd",
                (System.nanoTime() - startTime) / 1e6,
                ackPacket.isSyn() ? "S" : "-",
                ackPacket.isAck() ? "A" : "-",
                ackPacket.isFin() ? "F" : "-",
                ackPacket.getPayload().length > 0 ? "D" : "-",
                ackPacket.getSequenceNumber(),
                ackPacket.getPayload().length,
                ackPacket.getAcknowledgement()
            );
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

        for (int i = 0; i < MAX_RETRIES; i++) {
            TCPPacket finAckPacket = new TCPPacket(seqNo, nextByte);
            finAckPacket.setFlag(TCPFlag.FIN, true);

            byte[] buf = finAckPacket.serialize();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr);

            System.out.printf(FORMAT,
                "snd",
                (System.nanoTime() - startTime) / 1e6,
                finAckPacket.isSyn() ? "S" : "-",
                finAckPacket.isAck() ? "A" : "-",
                finAckPacket.isFin() ? "F" : "-",
                finAckPacket.getPayload().length > 0 ? "D" : "-",
                finAckPacket.getSequenceNumber(),
                finAckPacket.getPayload().length,
                finAckPacket.getAcknowledgement()
            );
            socket.send(packet);

            // GET ACK
            socket.setSoTimeout(INITIAL_TIMEOUT);

            try {
                buf = new byte[config.mtu()];
                DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
                socket.receive(ackPacket);
                TCPPacket pkt = (TCPPacket)(new TCPPacket()).deserialize(buf);
                System.out.printf(FORMAT,
                    "snd",
                    (System.nanoTime() - startTime) / 1e6,
                    pkt.isSyn() ? "S" : "-",
                    "A",
                    "-",
                    pkt.getPayload().length > 0 ? "D" : "-",
                    pkt.getSequenceNumber(),
                    pkt.getPayload().length,
                    pkt.getAcknowledgement()
                );
            } catch (SocketTimeoutException e) {
                continue;
            }
            return;
        }

        throw new IllegalStateException("Failed to properly close");
    }
}
