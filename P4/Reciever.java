import java.net.*;
import java.util.*;
import java.io.*;

class Receiver {
    private int port;
    private DatagramSocket datagramSocket;
    private Map<SocketAddress, ConnectionState> connections;

    // Class to keep track of the expected sequence number and address of the sender
    private class ConnectionState {
        int expectedSequenceNumber;
        boolean isClosing; // new field to track if the connection is closing

        ConnectionState(int initialSequenceNumber) {
            this.expectedSequenceNumber = initialSequenceNumber;
            this.isClosing = false; // default value for new connections
        }
    }

    public Receiver(int port) {
        this.port = port;
        this.connections = new HashMap<>();
    }

    public void start() {
        try {
            // Construct and bind socket to the given port
            datagramSocket = new DatagramSocket(port);
            System.out.println("Datagram socket listening on port " + port);

            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(datagramPacket);
                TCPPacket tcpPacket = new TCPPacket();

                tcpPacket = tcpPacket.deserialize(datagramPacket.getData());

                if (tcpPacket == null) {
                    System.out.println("Invalid packet received");
                    continue;
                }

                SocketAddress senderAddress = datagramPacket.getSocketAddress();

                if (!connections.containsKey(senderAddress)) {
                    // First connection, set up a new connection state
                    if (tcpPacket.getSequenceNumber() == 0) {
                        connections.put(senderAddress, new ConnectionState(1)); // next sequence number = 1
                    } else {
                        System.out.println("Unexpected initial sequence number. Ignoring packet.");
                        continue;
                    }
                }

                ConnectionState connection = connections.get(senderAddress);

                // Handle connection termination
                if (tcpPacket.isFin()) {
                    System.out.println("Connection termination request received");
                    // Acknowledge the FIN packet
                    sendAcknowledgment(tcpPacket, senderAddress);

                    // Mark the connection as closing
                    connection.isClosing = true;

                    // Remove the connection after sending the acknowledgment
                    connections.remove(senderAddress);
                    System.out.println("Connection closed with " + senderAddress);
                    continue;
                }

                if (tcpPacket.getSequenceNumber() == connection.expectedSequenceNumber) {
                    processPacket(tcpPacket, senderAddress);
                } else {
                    System.out.println("Unexpected sequence number. Expected: " + connection.expectedSequenceNumber + ", but got: " + tcpPacket.getSequenceNumber());
                }
            }
        } catch (IOException e) {
            System.err.println("Error in TCPReceiver: " + e.getMessage());
        }
    }

    private void processPacket(TCPPacket packet, SocketAddress senderAddress) {
        ConnectionState connection = connections.get(senderAddress);

        System.out.println("Received packet with sequence number: " + packet.getSequenceNumber());

        // Send acknowledgment
        sendAcknowledgment(packet, senderAddress);

        // Sequence number incremented
        connection.expectedSequenceNumber++;
    }

    private void sendAcknowledgment(TCPPacket packet, SocketAddress senderAddress) {
        int ackNumber = packet.getSequenceNumber() + packet.getLength();
        TCPPacket ackPacket = new TCPPacket(ackNumber);
        ackPacket.setAcknowledgment(ackNumber);
        ackPacket.setTimestamp(packet.getTimestamp()); // for RTT calculation

        byte[] ackData = ackPacket.serialize();
        DatagramPacket ackDatagram = new DatagramPacket(ackData, ackData.length, senderAddress);

        try {
            datagramSocket.send(ackDatagram);
        } catch (IOException e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }
    }
}
