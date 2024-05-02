import java.io.IOException;

public interface IServer {
    public void bind() throws IOException;
    public void accept() throws IOException;
    public void start() throws IOException;
    public void close() throws IOException;
}
