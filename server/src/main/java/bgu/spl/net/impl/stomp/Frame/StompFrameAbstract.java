package bgu.spl.net.impl.stomp.Frame;

import java.util.HashMap;

public abstract class StompFrameAbstract {
    protected String command;
    protected HashMap<String, String> headers;
    protected String body;
    protected int msgId=1;

    protected StompFrameAbstract(String inCommand){
        this.command = inCommand;
        this.headers = new HashMap<>();
        this.body = "";
        this.msgId++;
    }

    public String getCommand() {
        return command;
    }
    public int getMsgId(){
        return msgId;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String inBody) {
        this.body = inBody;
    }

    public String toString() {
        StringBuilder ans = new StringBuilder();
        ans.append(command).append("\n");
        for (HashMap.Entry<String, String> entry : headers.entrySet()) {
            ans.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        ans.append("\n"); // Empty line between headers and body
        if (body != "") {
            ans.append(body).append("\n");
        }
        ans.append((char) 0); // Null character to terminate the frame
        return ans.toString();
    }

}
