public class Iperfer {

    private final static String USAGE_MSG = "Usage: java Iperfer -c -h"
        + " <server hostname> -p <server port> -t <time>";

    private static void printUsage() {
        System.out.println(Iperfer.USAGE_MSG);
    }

    private static void printErrorMsg(String msg) {
        System.out.println(msg);
    }

    public static void main(String[] args) {
        System.out.println(java.util.Arrays.toString(args));
        IperferArgParser parser = new IperferArgParser();

        try {
            parser.parse(args);
        } catch (IperferArgParser.ArgsNotParsedException e) {
            System.out.println(e.getMessage());
            return;
        }

        NetworkTest session;

        if (parser.isClientMode()) {
            session = new IperferClient(parser.getClientConfig());
        } else if (parser.isServerMode()) {
            session = new IperferServer(parser.getServerConfig());
        } else {
            System.out.println("Failed to parse args!");
            return;
        }

        session.startSession();
    }
}
