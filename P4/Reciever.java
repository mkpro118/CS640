import java.net.*;
import java.util.*;
import java.io.*;

class Receiver {
    private int port;
    private DatagramSocket datagramSocket;
    private Map<SocketAddress, ConnectionState> connections;

    // class to keep track of the expected sequence number and address of sender
    private class ConnectionState {
        int expectedSequenceNumber;
        SocketAddress senderAddress;

        ConnectionState(SocketAddress senderAddress, int initialSequenceNumber) {
            this.expectedSequenceNumber = initialSequenceNumber;
            this.senderAddress = senderAddress;
        }
    }

    public Receiver(int port) {
        this.port = port;
        this.connections = new HashMap<>();
    }

    public void start() {
        try {
            //construct and bind socket to given port
            datagramSocket = new DatagramSocket(port);
            System.out.println("Datagram socket listening on port " + port);

            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(datagramPacket);

                TCPPacket tcpPacket = TCPPacket.deserialize(datagramPacket.getData());

                if (tcpPacket == null) {
                    System.out.println("Invalid packet received");
                    continue;
                }

                SocketAddress senderAddress = datagramPacket.getSocketAddress();

                if (!connections.containsKey(senderAddress)) {
                    // first connection, set up new connection state
                    if (tcpPacket.getSequenceNumber() == 0) {
                        connections.put(senderAddress, new ConnectionState(senderAddress, 1)); // next sequence number = 1
                    } else {
                        System.out.println("Unexpected initial sequence number. Ignoring packet.");
                        continue;
                    }
                }

                ConnectionState connection = connections.get(senderAddress);

                if (tcpPacket.getSequenceNumber() == connection.expectedSequenceNumber) {
                    processPacket(tcpPacket, senderAddress);
                } else {
                    System.out.println("Unexpected sequence number, Expected:" + connection.expectedSequenceNumber + " but got " + tcpPacket.getSequenceNumber());
                }
            }
        } catch (IOException e) {
            System.err.println("Error in TCPReceiver:" + e.getMessage());
        }
    }

    private void processPacket(TCPPacket packet, SocketAddress senderAddress) {
        ConnectionState connection = connections.get(senderAddress);

        System.out.println("Received packet with sequence number: " + packet.getSequenceNumber());

        // sending ack
        TCPPacket ackPacket = new TCPPacket();
        ackPacket.setSequenceNumber(connection.expectedSequenceNumber); // expected sequence number
        ackPacket.setAcknowledgment(packet.getSequenceNumber() + packet.getLength()); // ack received data
        ackPacket.setTimestamp(packet.getTimestamp()); // timestamp for rtt calc
        //set flags
        
        byte[] ackData = ackPacket.serialize();
        DatagramPacket ackDatagram = new DatagramPacket(
            ackData, ackData.length, senderAddress);

        try {
            datagramSocket.send(ackDatagram);
        } catch (IOException e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }

        // sequence number incremented
        connection.expectedSequenceNumber++;
    }
}
