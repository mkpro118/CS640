import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectionUtils {
    private final static byte[] dataBuffer;

    static {
        dataBuffer = new byte[Constants.CHUNK_SIZE.getValue()];
    }

    public final static Socket createSocket(String hostname, int port) {
        try {
            return new Socket(hostname, port);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error creating socket: " + e.getMessage());
            System.exit(1);
        }

        return null;
    }

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

    public final static void sendData(Socket socket) {
        try (OutputStream outputStream = socket.getOutputStream()) {
            outputStream.write(dataBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error sending data: " + e.getMessage());
            System.exit(1);
        }
    }

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
