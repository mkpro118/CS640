public class IperferServer extends NetworkTest {
    ServerConfig config;
    private final static String serverFormat = "received=%d KB rate=%f Mbps\n";

    public IperferServer(ServerConfig config) {
        super(serverFormat);
        this.config = config;
    }

    @Override
    public void startSession() {

    }

    @Override
    public void startTest() {
        // TODO: Implement
    }
}
