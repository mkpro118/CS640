/**
 * @author Mrigank Kumar
 *
 * A specialized argument parser for the Iperfer application
 * This class extends the generic ArgParser class to provide parsing
 * functionality specific to the Iperfer application's command-line arguments
 */
public class IperferArgParser extends ArgParser {
    /**
     * Custom exception class for indicating that arguments have not been parsed
     */
    @SuppressWarnings("serial")
    public static class ArgsNotParsedException extends RuntimeException {
        public ArgsNotParsedException(String msg) {
            super(msg);
        }
    }

    // Error message for missing or additional arguments
    private final static ArgsNotParsedException ERROR_MISSING_ARGS =
        new ArgsNotParsedException("Error: missing or additional arguments");

    // Error message for an invalid port number
    private final static ArgsNotParsedException ERROR_INVALID_PORT =
        new ArgsNotParsedException("Error: port number must be in the range "
            + "1024 to 65535");

    // Error message for attempting to access parsed arguments before parsing
    private final static ArgsNotParsedException ERROR_PARSE_FIRST =
        new ArgsNotParsedException("Options have not been parsed. "
                + "Use ArgParser.parse(String[]) first");

    // Minimum allowed port number
    private final static int PORT_MIN = 1 << 10;
    // Maximum allowed port number
    private final static int PORT_MAX = (1 << 16) - 1;

    // Configuration object for the client mode
    private ClientConfig clientConfig;
    // Configuration object for the server mode
    private ServerConfig serverConfig;

    // Flag indicating whether arguments have been parsed
    private boolean parsed;

    /**
     * Constructs an IperferArgParser object and initializes
     * the available options
     */
    public IperferArgParser() {
        super();
        clientConfig = null;
        serverConfig = null;
        parsed = false;

        // Define available options for Iperfer
        this.addOption("-c", "boolean", "Client Mode")
            .addOption("-s", "boolean", "Server Mode")
            .addOption("-h", "String", "Hostname")
            .addOption("-p", "int", "Port Number")
            .addOption("-t", "int", "Test Duration");
    }

    /**
     * Parses the command-line arguments specific to the Iperfer application
     *
     * @param args the command-line arguments
     *
     * @throws ArgsNotParsedException if arguments have not been parsed yet
     * @throws ArgsNotParsedException if required arguments are missing or
     *                         `      additional arguments are present
     * @throws ArgsNotParsedException if the port number is out of range
     */
    @Override
    public void parse(String[] args) {
        super.parse(args);

        parsed = true;

        // Port number must always be specified
        if (!getOption("-p").found())
            throw ERROR_MISSING_ARGS;

        int port = get("-p");

        // Validate port number
        if (port < PORT_MIN || port > PORT_MAX)
            throw ERROR_INVALID_PORT;

        boolean isClient = get("-c");
        boolean isServer = get("-s");

        // If both or neither `-c` and `-s` are given, operation mode is
        // ambiguous
        if (isClient == isServer)
            throw ERROR_MISSING_ARGS;
        else if (isClient){
            // Client mode requires hostname and duration
            if (!getOption("-h").found() || !getOption("-t").found())
                throw ERROR_MISSING_ARGS;

            String hostName = get("-h");
            int duration = get("-t");

            clientConfig = new ClientConfig(hostName, port, duration);
        }
        else {
            // Server mode only requires the listen port
            serverConfig = new ServerConfig(port);
        }
    }

    /**
     * Checks if the parser is in client mode
     *
     * @return true if the parser is in client mode, otherwise false
     *
     * @throws ArgsNotParsedException if arguments have not been parsed yet
     */
    public boolean isClientMode() {
        checkParsed();
        return clientConfig != null;
    }

    /**
     * Checks if the parser is in server mode
     *
     * @return true if the parser is in server mode, otherwise false
     *
     * @throws ArgsNotParsedException if arguments have not been parsed yet
     */
    public boolean isServerMode() {
        checkParsed();
        return serverConfig != null;
    }

    /**
     * Accessor for the client configuration
     *
     * @return the client configuration object
     *
     * @throws ArgsNotParsedException if arguments have not been parsed yet
     */
    public ClientConfig getClientConfig() {
        checkParsed();

        return clientConfig;
    }

    /**
     * Accessor for the server configuration
     *
     * @return the server configuration object
     *
     * @throws ArgsNotParsedException if arguments have not been parsed yet
     */
    public ServerConfig getServerConfig() {
        checkParsed();

        return serverConfig;
    }

    /**
     * Checks if arguments have been parsed
     *
     * @throws ArgsNotParsedException if arguments have not been parsed yet
     */
    private final void checkParsed() {
        if (!parsed)
            throw ERROR_PARSE_FIRST;
    }
}
