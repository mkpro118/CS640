import java.io.IOException;

/**
 * @author Arushi Mishra
 *
 * Interface specifying a server/receiver
 */
public interface IServer {
    public void bind() throws IOException;
    public void accept() throws IOException;
    public void start() throws IOException;
    public void close() throws IOException;
}
