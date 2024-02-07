public class IperferClient extends NetworkTest {
    ClientConfig config;
    private final static String clientFormat = "sent=%d KB rate=%f Mbps\n";

    public IperferClient(ClientConfig config) {
        super(clientFormat);
        this.config = config;
    }

    @Override
    public void startSession() {
        // Implement client logic to send data
    }

    public void stopSession() {
        // Implement client termination logic
    }

    @Override
    public void startTest() {
        // TODO: Implement
    }
}
