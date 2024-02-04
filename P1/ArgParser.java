public class ArgParser {
    private boolean isClientMode;
    private int serverPort;
    private int time;
    private int listenPort;
    private String serverHostname;

    public ArgParser(String[] args) {
        // TODO: Parse the arguments and initialize the fields
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
