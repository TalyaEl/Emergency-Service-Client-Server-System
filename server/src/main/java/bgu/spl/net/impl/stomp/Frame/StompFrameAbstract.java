package bgu.spl.net.impl.stomp.Frame;

import java.util.HashMap;

public abstract class StompFrameAbstract {
    protected String command;
    protected HashMap<String, String> headers;
    protected String body;

    protected StompFrameAbstract(String inCommand){
        this.command = inCommand;
        this.headers = new HashMap<>();
        this.body = "";
    }

    public String getCommand() {
        return command;
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
        ans.append(this.command + "\n");
        for (String key : this.headers.keySet()) {
            String value = this.headers.get(key);
            ans.append(key + ":" + value + "\n");
        }
        ans.append("\n"); // Empty line between headers and body
        if (body != "") {
            ans.append(body + "\n");
        }
        ans.append("\n" + '\0'); // Null character to terminate the frame
        return ans.toString();
    }

    public byte[] encode() {
        return toString().getBytes();
    }

}
