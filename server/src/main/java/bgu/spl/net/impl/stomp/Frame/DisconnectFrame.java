package bgu.spl.net.impl.stomp.Frame;

public class DisconnectFrame extends StompFrameAbstract {
    public DisconnectFrame(int receiptID) {
        super("DISCONNECT");
        this.headers.put("receipt:", String.valueOf(receiptID));
    }

}
