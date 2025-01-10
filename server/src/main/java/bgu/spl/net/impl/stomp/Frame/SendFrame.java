package bgu.spl.net.impl.stomp.Frame;

public class SendFrame extends StompFrameAbstract {
    public SendFrame(String inBody, String topic) {
        super("SEND");
        this.headers.put("destination:", topic);
        this.body = inBody;
    }
}
