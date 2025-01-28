package bgu.spl.net.impl.stomp.Frame;

import java.util.concurrent.atomic.AtomicInteger;

public class MessageFrame extends StompFrameAbstract {
    private AtomicInteger messageId = new AtomicInteger(1);
    public MessageFrame(int subId, String dest, String inBody) {
        super("MESSAGE");
        this.headers.put("subscription", String.valueOf(subId));
        this.headers.put("message-id", String.valueOf(messageId.getAndIncrement()));
        this.headers.put("destination", dest);
        this.body = inBody;
    }
    
}
