package bgu.spl.net.impl.stomp.Frame;

public class ConnectFrame extends StompFrameAbstract {
    public ConnectFrame(String user, String passcode) {
        super("CONNECT");
        this.headers.put("accept-version", "1.2");
        this.headers.put("host", "stomp.cs.bgu.ac.il");
        this.headers.put("login", user);
        this.headers.put("passcode", passcode);
    }
}
