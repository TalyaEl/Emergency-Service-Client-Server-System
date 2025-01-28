package bgu.spl.net.impl.stomp.Frame;

public class ReceiptFrame extends StompFrameAbstract {
    public ReceiptFrame(int clientID) {
        super("RECEIPT");
        this.headers.put("receipt-id", String.valueOf(clientID));
    }
}
