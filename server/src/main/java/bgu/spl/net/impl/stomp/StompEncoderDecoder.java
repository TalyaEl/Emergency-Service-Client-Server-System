package bgu.spl.net.impl.stomp;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.stomp.Frame.ConnectFrame;
import bgu.spl.net.impl.stomp.Frame.ConnectedFrame;
import bgu.spl.net.impl.stomp.Frame.DisconnectFrame;
import bgu.spl.net.impl.stomp.Frame.ErrorFrame;
import bgu.spl.net.impl.stomp.Frame.MessageFrame;
import bgu.spl.net.impl.stomp.Frame.ReceiptFrame;
import bgu.spl.net.impl.stomp.Frame.SendFrame;
import bgu.spl.net.impl.stomp.Frame.StompFrameAbstract;
import bgu.spl.net.impl.stomp.Frame.SubscribeFrame;
import bgu.spl.net.impl.stomp.Frame.UnsubscribeFrame;

public class StompEncoderDecoder implements MessageEncoderDecoder<StompFrameAbstract> {
    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    @Override
    public StompFrameAbstract decodeNextByte(byte nextByte) {
        buffer.put(nextByte);
        if (nextByte == '\0') {
            buffer.flip();
            StompFrameAbstract frame = decodeFrame(buffer);
            buffer.clear();
            return frame;
        }
        return null;
    }

    private StompFrameAbstract decodeFrame(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes); //copying the remaining buffer to the array
        //to identify whether or not the frame we're trying to create is valid
        String frameString = new String(bytes, StandardCharsets.UTF_8).trim(); //creating a frame represented by string
        String[] lines = frameString.split("\n"); //converting the string to array according to the new line
        if (lines.length < 2) {
            throw new IllegalArgumentException("invalid stomp frame");
        }

        String command = lines[0]; //the first line is always the sommand
        ConcurrentHashMap<String, String> headers = parseHeaders(lines);
        String body = parseBody(lines);
        StompFrameAbstract frame = createFrame(command, headers, body);
        return frame;

        // String commandLine = new String(bytes, 0, findEndOfLine(bytes, 0), StandardCharsets.UTF_8).trim();
        // StompFrameAbstract frame = null;


    }

    private ConcurrentHashMap<String, String> parseHeaders(String[] lines) {
        ConcurrentHashMap<String, String> ansHeaders = new ConcurrentHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim(); //ignoring any extra spaces
            if (line.isEmpty()) { //the space between the headers and the body of the frame, if we got here we have the headers
                break;
            }
            int colonIndex = line.indexOf(':'); //define the first and second part of the header
            if (colonIndex != -1) {
                String headerName = line.substring(0, colonIndex).trim();
                String headerVal = line.substring(colonIndex + 1).trim();
                ansHeaders.put(headerName, headerVal);
            }
        }
        return ansHeaders;
    }

    private String parseBody(String[] lines) {
        StringBuilder bodybuild = new StringBuilder();
        int bodyStartIndex = -1;
        //checking where is the end of the headers, between the headers and the body there's an empty line
        for (int i = 1; i < lines.length; i++) { 
            if (lines[i].trim().isEmpty()) {
                bodyStartIndex = i + 1;
                break;
            }
        }

        if (bodyStartIndex == -1 || bodyStartIndex >= lines.length) { //there's no body
            return "";
        }

        for (int i = bodyStartIndex; i < lines.length; i++) { //creating the body
            bodybuild.append(lines[i].trim()).append("\n");
        }
        return bodybuild.toString();
    }

    private StompFrameAbstract createFrame(String command, ConcurrentHashMap<String, String> headers, String body) {
        switch (command) {
            case "CONNECT":
                String user = headers.get("login:");
                String passcode = headers.get("passcode:");
                if (user == null || passcode == null) {throw new IllegalArgumentException("missing headers");}
                return new ConnectFrame(user, passcode);
            case "CONNECTED":
                return new ConnectedFrame();
            case "MESSAGE":
                String sub = headers.get("subscription:");
                String id = headers.get("message-id:");
                String dest = headers.get("destination:");
                if (sub == null || id == null || dest == null) {throw new IllegalArgumentException("missing headers");}
                return new MessageFrame(Integer.parseInt(sub), Integer.parseInt(id), dest, body);
            case "RECEIPT":
                String clientid = headers.get("receipt-id:");
                if (clientid == null) {throw new IllegalArgumentException("missing headers");} 
                return new ReceiptFrame(Integer.parseInt(clientid));
            case "ERROR":
                String msg = headers.get("message:");
                String receiptid = headers.get("receipt-id:");
                if (msg == null || receiptid == null) {throw new IllegalArgumentException("missing headers");} 
                return new ErrorFrame(msg, Integer.parseInt(receiptid), body, null);
            case "SEND":
                String topic = headers.get("destination:");
                if (topic == null) {throw new IllegalArgumentException("missing headers");} 
                return new SendFrame(body, topic);
            case "SUBSCRIBE":
                String topic2 = headers.get("destination:");
                String id2 = headers.get("id:");
                if (topic2 == null || id2 == null) {throw new IllegalArgumentException("missing headers");} 
                return new SubscribeFrame(topic2, Integer.parseInt(id2));
            case "UNSUBSCRIBE":
                String id3 = headers.get("id:");
                if (id3 == null) {throw new IllegalArgumentException("missing headers");}
                return new UnsubscribeFrame(Integer.parseInt(id3));
            case "DISCONNECT":
                String receiptid2 = headers.get("receipt:");
                if (receiptid2 == null) {throw new IllegalArgumentException("missing headers");}
                return new DisconnectFrame(Integer.parseInt(receiptid2));
        }
        return null;
    }

    // private int findEndOfLine(byte[] bytes, int startIndex) { //helper method
    //     for (int i = startIndex; i < bytes.length; i++) {
    //         if (bytes[i] == '\n') {
    //             return i + 1;
    //         }
    //     }
    //     return bytes.length;
    // }
    // private byte[] bytes = new byte[1 << 10];
    // private int len = 0;
    // private StompFrameAbstract currentFrame = null;

    // @Override
    // public StompFrameAbstract decodeNextByte(byte nextByte) {
    //     if (nextByte == '\0') { //if we're at the end of the frame
    //         if (currentFrame != null) { //and also if the frame we created isn't null
    //             StompFrameAbstract ansFrame = currentFrame;
    //             len = 0;
    //             currentFrame = null;
    //             return ansFrame;
    //         }
    //     }
    //     else {
    //         pushByte(nextByte);
    //     }
    //     return null;
    // }

    // private void pushByte(byte nextByte) {
    //     if (len >= bytes.length) {
    //         byte[] newBytes = new byte[bytes.length * 2];
    //         for (int i = 0; i <bytes.length; i++) {
    //             newBytes[i] = bytes[i];
    //         }
    //         bytes = newBytes;
    //     }
    //     bytes[len++] = nextByte;
    // }

    @Override
    public byte[] encode(StompFrameAbstract message) {
        return null;
    }
}
