package bgu.spl.net.impl.stomp.Frame;

public class UnsubscribeFrame extends StompFrameAbstract {
    public UnsubscribeFrame(int id) {
        super("UNSUBSCRIBE");
        this.headers.put("id", String.valueOf(id));
    }
}
