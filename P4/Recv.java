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

    private static final int MAX_RETRIES = 0x10;
    private static final int INITIAL_TIMEOUT = 0x1388;

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
            System.out.println(":accept: Received (synPacket) " + synPacket);
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

            System.out.println(":accept: Sending (synAckPacket) " + synAckPacket);
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
            System.out.println(":accept: Received (recvAckPacket) " + recvAckPacket);

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
                System.out.println("Connection Established!");
                return;
            } else {
                System.out.println("recvAckPacket.getAcknowledgement() " + recvAckPacket.getAcknowledgement());
            }
        }

        throw new IllegalStateException("Failed to accept");
    }

    @Override
    public void start() throws IOException {
        worker.start();
        byte[] buf;
        DatagramPacket pkt;
        System.out.println("connect = " + isConnected);
        while (isConnected) {
            buf = new byte[config.mtu()];
            pkt = new DatagramPacket(buf, buf.length);

            socket.receive(pkt);

            TCPPacket dataPacket = new TCPPacket();
            dataPacket = (TCPPacket) dataPacket.deserialize(buf);

            if (dataPacket.isFin()) {
                isConnected = false;
                TCPPacket ackPacket = new TCPPacket(seqNo, nextByte);
                ackPacket.setFlag(TCPFlag.ACK, true);
                System.out.println("Sending ACK " + ackPacket);
                buf = ackPacket.serialize();
                pkt = new DatagramPacket(buf, buf.length, serverAddr);
                socket.send(pkt);
                return;
            }

            if (dataPacket.isSyn() || dataPacket.isAck()) {
                System.out.println("Unexpected SYN/ACK packet");
                System.out.println(dataPacket);
                continue;
            }

            if (dataPacket.getSequenceNumber() != nextByte) {
                System.out.println("Wrong Seqno, expected " + nextByte);
                System.out.println(dataPacket);
                continue;
            }

            if (workQueue.offer(dataPacket))
                nextByte += dataPacket.getPayload().length;

            TCPPacket ackPacket = new TCPPacket(seqNo, nextByte);
            ackPacket.setFlag(TCPFlag.ACK, true);
            System.out.println("Sending ACK " + ackPacket);
            buf = ackPacket.serialize();
            pkt = new DatagramPacket(buf, buf.length, serverAddr);
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
            System.out.println("length = " + packet.getPayload().length);
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

        TCPPacket synAckPacket = new TCPPacket(seqNo, nextByte);
        synAckPacket.setFlag(TCPFlag.FIN, true);
        synAckPacket.setFlag(TCPFlag.ACK, true);

        byte[] buf = synAckPacket.serialize();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr);

        socket.send(packet);

        // GET ACK
        socket.setSoTimeout(INITIAL_TIMEOUT);

        try {
            buf = new byte[config.mtu()];
            DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
            socket.receive(ackPacket);
        } catch (SocketTimeoutException e) {
            return;
        }
    }
}
