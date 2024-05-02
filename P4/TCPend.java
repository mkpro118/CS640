public final class TCPend {
    public final static void main(String[] args) {
        ArgParser parser = new ArgParser();

        parser.addOption("-p", "int")
              .addOption("-s", "string")
              .addOption("-a", "int")
              .addOption("-f", "string")
              .addOption("-m", "int")
              .addOption("-c", "int");

        parser.parse(args);

        if (parser.getOrDefault("-a", -1) == -1) {
            runReceiver(new RecvConfig(
                parser.get("-p"),
                parser.get("-m"),
                parser.get("-c"),
                parser.get("-f")
            ));
        } else {
            runSender(new SendConfig(
                parser.get("-p"),
                parser.get("-s"),
                parser.get("-a"),
                parser.get("-f"),
                parser.get("-m"),
                parser.get("-c")
            ));
        }
    }

    private final static void runReceiver(RecvConfig config) {
        Receiver receiver = new Receiver(config);
        receiver.accept();
        receiver.start();
    }

    private final static void runSender(SendConfig config) {
        Sender sender = new Sender(config);
        sender.connect();
        sender.sendFile();
        sender.close();
    }
}
