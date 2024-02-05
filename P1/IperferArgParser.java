public class IperferArgParser extends ArgParser {
    private boolean isClientMode;
    private int serverPort;
    private int time;
    private int listenPort;
    private String serverHostname;

    public IperferArgParser() {
        isClientMode = false;
        serverPort = 0;
        time = 0;
        listenPort = 0;
        serverHostname = null;
    }

    public boolean isClientMode() {
        return isClientMode;
    }

    public String getServerHostname() {
        return serverHostname;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getTime() {
        return time;
    }

    public int getListenPort() {
        return listenPort;
    }
}
