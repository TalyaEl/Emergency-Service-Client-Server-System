package bgu.spl.net.impl.stomp.Frame;

public class MessageFrame extends StompFrameAbstract {
    public MessageFrame(int clientID, int serverId, String dest, String inBody){
        super("MESSAGE");
        this.headers.put("subscription:", String.valueOf(clientID));
        this.headers.put("message-id:", String.valueOf(serverId));
        this.headers.put("destination:", dest);
        this.body = inBody;
    }
}
