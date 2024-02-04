public class Iperfer {

    private final static String USAGE_MSG = "Usage: java Iperfer -c -h"
        + " <server hostname> -p <server port> -t <time>";

    private static void printUsage() {
        System.out.println(Iperfer.USAGE_MSG);
    }


    public static void main(String[] args) {
        printUsage();
    }
}
