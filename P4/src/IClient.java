import java.io.IOException;

/**
 * @author Mrigank Kumar
 *
 * Interface specifying a client/sender
 */
public interface IClient {
    public void connect() throws IOException;
    public void sendFile() throws IOException;
    public void close() throws IOException ;
}
