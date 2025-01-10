package bgu.spl.net.impl.stomp.Frame;

public class ConnectedFrame extends StompFrameAbstract {
    public ConnectedFrame() {
        super("CONNECTED");
        this.headers.put("version:", "1.2");
    }
}
