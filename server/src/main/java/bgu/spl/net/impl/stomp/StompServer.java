package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: StompServer <port> <server type (tpc/reactor)>");
            return;
        }
    
        // Parse the port
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid port number");
            return;
        }
    
        // Check server type
        String serverType = args[1];
        if (!serverType.equals("tpc") && !serverType.equals("reactor")) {
            System.out.println("Server type must be either 'tpc' or 'reactor'");
            return;
        }

        if (serverType.equals("tpc")) {
            Server.threadPerClient(
                port, //port
                () -> new StompProtocol(), //protocol factory
                StompEncoderDecoder::new //message encoder decoder factory
            ).serve();
        }
        else if (serverType.equals("reactor")) {
            Server.reactor(
                Runtime.getRuntime().availableProcessors(),
                port, //port
                () -> new StompProtocol(), //protocol factory
                StompEncoderDecoder::new //message encoder decoder factory
            ).serve();
        }
        else {
            System.out.println("args not valid");
        }
    }
}
