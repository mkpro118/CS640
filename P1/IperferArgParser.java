public class IperferArgParser extends ArgParser {
    @SuppressWarnings("serial")
    private static class ArgsNotParsedException extends RuntimeException {
        public ArgsNotParsedException(String msg) {
            super(msg);
        }
    }

    private final static ArgsNotParsedException ERROR_MISSING_ARGS =
        new ArgsNotParsedException("Error: missing or additional arguments");

    private final static ArgsNotParsedException ERROR_INVALID_PORT =
        new ArgsNotParsedException("Error: port number must be in the range "
            + "1024 to 65535");

    private final static ArgsNotParsedException ERROR_PARSE_FIRST =
        new ArgsNotParsedException("Options have not been parsed. "
                + "Use ArgParser.parse(String[]) first");

    private final static int PORT_MIN = 1 << 10;
    private final static int PORT_MAX = (1 << 16) - 1;

    private ClientConfig clientConfig;
    private ServerConfig serverConfig;
    private boolean parsed;

    public IperferArgParser() {
        super();
        clientConfig = null;
        serverConfig = null;
        parsed = false;

        this.addOption("-c", "boolean", "Client Mode")
            .addOption("-s", "boolean", "Server Mode")
            .addOption("-h", "String", "Hostname")
            .addOption("-p", "int", "Port Number")
            .addOption("-t", "int", "Test Duration");
    }

    @Override
    public void parse(String[] args) {
        super.parse(args);

        parsed = true;

        if (!getOption("-p").found())
            throw ERROR_MISSING_ARGS;

        int port = get("-p");

        if (port < PORT_MIN || port > PORT_MAX)
            throw ERROR_INVALID_PORT;

        boolean isClient = get("-c");
        boolean isServer = get("-s");

        if (isClient == isServer)
            throw ERROR_MISSING_ARGS;
        else if (isClient){
            if (!getOption("-h").found() || !getOption("-t").found())
                throw ERROR_MISSING_ARGS;

            String hostName = get("-h");
            int duration = get("-t");

            clientConfig = new ClientConfig(hostName, port, duration);
        }
        else {
            serverConfig = new ServerConfig(port);
        }
    }

    public boolean isClientMode() {
        checkParsed();
        return clientConfig != null;
    }

    public boolean isServerMode() {
        checkParsed();
        return serverConfig != null;
    }

    public String getHostname() {
        checkParsed();

        if (isClientMode())
            return clientConfig.hostname();

        return null;
    }

    public int getServerPort() {
        checkParsed();

        if (isClientMode())
            return clientConfig.serverPort();

        return -1;
    }

    public int getTime() {
        checkParsed();

        if (isClientMode())
            return clientConfig.time();

        return -1;
    }

    public int getListenPort() {
        checkParsed();

        if (isServerMode())
            return serverConfig.listenPort();

        return -1;
    }

    private final void checkParsed() {
        if (!parsed)
            throw ERROR_PARSE_FIRST;
    }
}
