package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.impl.stomp.Frame.ConnectFrame;
import bgu.spl.net.impl.stomp.Frame.ConnectedFrame;
import bgu.spl.net.impl.stomp.Frame.DisconnectFrame;
import bgu.spl.net.impl.stomp.Frame.ErrorFrame;
import bgu.spl.net.impl.stomp.Frame.ReceiptFrame;
import bgu.spl.net.impl.stomp.Frame.SendFrame;
import bgu.spl.net.impl.stomp.Frame.StompFrameAbstract;
import bgu.spl.net.impl.stomp.Frame.SubscribeFrame;
import bgu.spl.net.impl.stomp.Frame.UnsubscribeFrame;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;

public class StompProtocol implements MessagingProtocol<StompFrameAbstract> {
    
    private boolean shouldTerminate = false;
    private Connections<StompFrameAbstract> connections;
    private int connectionId;

    @Override
    public void start(int connectionId, Connections<StompFrameAbstract> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public StompFrameAbstract process(StompFrameAbstract msg) {
        String command = msg.getCommand();
        switch (command) {
            case "CONNECT":
                return connect((ConnectFrame) msg);
            case "SEND":
                return send((SendFrame) msg);
            case "SUBSCRIBE":
                return subscribe((SubscribeFrame) msg);
            case "UNSUBSCRIBE":
                return unsubscribe((UnsubscribeFrame) msg);
            case "DISCONNECT":
                return disconnect((DisconnectFrame) msg);
            default:
                return new ErrorFrame("unknown command:" + command, connectionId, null, msg); 
                
        }
    }

    private StompFrameAbstract connect(ConnectFrame frame) {
        String login = frame.getHeaders().get("login");
        String passcode = frame.getHeaders().get("passcode");
        if (login == null || passcode == null) {
            return new ErrorFrame("missing login or passcode", connectionId, null, frame);
        }
        return new ConnectedFrame();
    }

    private StompFrameAbstract send(SendFrame frame) {
        String dest = frame.getHeaders().get("destination");
        if (dest == null) {
            ErrorFrame error = new ErrorFrame("missing destination", connectionId, null, frame);
            connections.send(connectionId, error);
            connections.disconnect(connectionId);
        }
        connections.send(dest, frame);
        return null; //need to handle the errors here like the instructions

    }

    private StompFrameAbstract subscribe(SubscribeFrame frame) { //IMPLEMENT
        return null;
    }

    private StompFrameAbstract unsubscribe(UnsubscribeFrame frame) { //IMPLEMENT
        return null;
    }

    private StompFrameAbstract disconnect(DisconnectFrame frame) {
        this.shouldTerminate = true;
        String receiptId = frame.getHeaders().get("receipt");
        connections.disconnect(connectionId);
        return new ReceiptFrame(Integer.parseInt(receiptId));
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

}
