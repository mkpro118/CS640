public class Iperfer {

    private final static String USAGE_MSG = "Usage: java Iperfer -c -h"
        + " <server hostname> -p <server port> -t <time>";

    private final static String ERROR_MISSING_ARGS = "Error: missing or "
        + "additional arguments";

    private final static String ERROR_INVALID_PORT = "Error: port number must "
        + "be in the range 1024 to 65535";

    private static void printUsage() {
        System.out.println(Iperfer.USAGE_MSG);
    }

    private static void printErrorMsg(String msg) {
        System.out.println(msg);
    }

    public static void main(String[] args) {
        System.out.println(java.util.Arrays.toString(args));
        ArgParser parser = new ArgParser();

        parser.addOption("-s", "boolean");
        parser.addOption("-c", "boolean");
        parser.addOption("-h", "string");
        parser.addOption("-p", "int");
        parser.addOption("-t", "int");

        parser.parse(args);

        boolean s = parser.get("-s");
        System.out.println(s);
        boolean c = parser.get("-c");
        System.out.println(c);
        String h = parser.get("-h");
        System.out.println(h);
        int p = parser.get("-p");
        System.out.println(p);
        int t = parser.get("-t");
        System.out.println(t);
    }
}
