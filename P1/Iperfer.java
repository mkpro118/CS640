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

        parser.parse(args);

        System.out.println("isClientMode() = " + parser.isClientMode());
        System.out.println("isServerMode() = " + parser.isServerMode());

        if (parser.isClientMode()) {
            System.out.println("Server Host Name: " + parser.getHostname());
            System.out.println("Server Port: " + parser.getServerPort());
            System.out.println("Time: " + parser.getTime());
        } else if (parser.isServerMode()) {
            System.out.println("Listen Port: " + parser.getListenPort());
        } else {
            System.out.println("Failed!");
        }
    }
}
