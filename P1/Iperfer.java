public class Iperfer {

    private final static String USAGE_MSG = "Usage: java Iperfer -[c|s] -h"
        + " <server hostname> -p <server port> -t <time>";

    private static void printUsage() {
        System.out.println(Iperfer.USAGE_MSG);
    }

    private static void printErrorMsg(String msg) {
        System.out.println(msg);
    }

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
