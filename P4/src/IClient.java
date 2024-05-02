import java.io.IOException;

public interface IClient {
    public void connect() throws IOException;
    public void sendFile() throws IOException;
    public void close() throws IOException ;
}
