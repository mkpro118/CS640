/**
 * @author Mrigank Kumar
 *
 * The main class for the Iperfer application
 * This class contains the main method responsible for parsing command-line
 * arguments, determining the mode of operation (client or server),
 * and initiating the appropriate network test session
 */
public class Iperfer {
    // The usage message for the Iperfer application
    private final static String USAGE_MSG = "Usage: java Iperfer -[c|s] -h"
        + " <server hostname> -p <server port> -t <time>";

    /**
     * Prints the usage message for the Iperfer application
     */
    private static void printUsage() {
        System.out.println(Iperfer.USAGE_MSG);
    }

    /**
     * Prints an error message
     *
     * @param msg the error message to be printed
     */
    private static void printErrorMsg(String msg) {
        System.out.println(msg);
    }

    /**
     * The main method of the Iperfer application
     *
     * Parses the command-line arguments, initializes the appropriate
     * network test session, and starts the session
     *
     * @param args the command-line arguments passed to the program
     */
    public static void main(String[] args) {
        IperferArgParser parser = new IperferArgParser();

        try {
            parser.parse(args);
        } catch (IperferArgParser.ArgsNotParsedException e) {
            printErrorMsg(e.getMessage());
            return;
        }

        NetworkTest session;

        if (parser.isClientMode()) {
            session = new IperferClient(parser.getClientConfig());
        } else if (parser.isServerMode()) {
            session = new IperferServer(parser.getServerConfig());
        } else {
            printErrorMsg("Failed to parse args!");
            printUsage();
            return;
        }

        session.startSession();
    }
}
