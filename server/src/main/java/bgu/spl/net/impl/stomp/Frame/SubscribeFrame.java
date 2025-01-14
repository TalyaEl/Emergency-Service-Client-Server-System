package bgu.spl.net.impl.stomp.Frame;

public class SubscribeFrame extends StompFrameAbstract {
    public SubscribeFrame(String topic, int id) {
        super("SUBSCRIBE");
        this.headers.put("destination", topic);
        this.headers.put("id", String.valueOf(id));
    }

}
