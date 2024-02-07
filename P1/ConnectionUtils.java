import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Mrigank Kumar
 *
 * Utility class for handling socket connections and data transfer
 * This class provides static methods for creating sockets,
 * sending and receiving data over sockets, and closing sockets
 */
public class ConnectionUtils {
    // Buffer for storing data to be sent or received
    private final static byte[] dataBuffer;

    // Initialize dataBuffer
    static {
        dataBuffer = new byte[Constants.CHUNK_SIZE.getValue()];
    }

    /**
     * Creates a client side socket connection with the given configurations
     *
     * @param config A client configuration object to set up the socket
     *
     * @return the created Socket object
     */
    public final static Socket createSocket(ClientConfig config) {
        try {
            Socket socket = new Socket();

            socket.connect(new InetSocketAddress(config.hostname(),
                                                 config.serverPort()));
            return socket;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error creating socket: " + e.getMessage());
            System.exit(1);
        }

        return null;
    }

    /**
     * Creates a server side socket connection with the given configurations
     *
     * @param config A server configuration object to set up the socket
     *
     * @return the created Socket object
     */
    public final static Socket createSocket(ServerConfig config) {
        try {
            Socket socket = new Socket();

            socket.bind(new InetSocketAddress(config.listenPort()));
            return socket;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error creating socket: " + e.getMessage());
            System.exit(1);
        }

        return null;
    }

    /**
     * Closes the specified socket
     *
     * @param socket the socket to be closed
     */
    public final static void closeSocket(Socket socket) {
        try {
            if (!socket.isClosed())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error closing socket: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Sends data over the specified socket
     *
     * @param socket the socket for sending data
     */
    public final static void sendData(Socket socket) {
        try (OutputStream outputStream = socket.getOutputStream()) {
            outputStream.write(dataBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error sending data: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Receives data from the specified socket
     *
     * @param socket the socket for receiving data
     *
     * @return the received data as a byte array
     */
    public final static byte[] receiveData(Socket socket) {
        try (InputStream inputStream = socket.getInputStream()) {
            inputStream.read(dataBuffer);

            return dataBuffer;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error receiving data: " + e.getMessage());
            System.exit(1);
        }

        return null;
    }
}
