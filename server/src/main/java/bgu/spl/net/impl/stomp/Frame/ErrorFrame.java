package bgu.spl.net.impl.stomp.Frame;

public class ErrorFrame extends StompFrameAbstract {
    public ErrorFrame(String error,int clientID, String inBody, StompFrameAbstract frame) {
        //inBody is the detailed info if there is(else it's NULL)
        //frame is the original message that caused the error
        super("ERROR");
        //if the frame has a receipt header than put it's value
        if (frame.getHeaders().containsKey("receipt")) {
            this.headers.put("receipt-id:", frame.getHeaders().get("receipt"));
        }
        else {
            this.headers.put("receipt-id:", String.valueOf(clientID));
        }
        this.headers.put("message:", error);
        //building the body according to the example
        StringBuilder bodyB = new StringBuilder();
        bodyB.append("The message:").append("\n");
        bodyB.append("-----").append("\n");
        bodyB.append(frame.toString()).append("\n");
        bodyB.append("-----").append("\n");
        bodyB.append(inBody);
        this.body = bodyB.toString();
    }
}
