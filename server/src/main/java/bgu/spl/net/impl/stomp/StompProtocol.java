package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.impl.stomp.Frame.ConnectFrame;
import bgu.spl.net.impl.stomp.Frame.ConnectedFrame;
import bgu.spl.net.impl.stomp.Frame.DisconnectFrame;
import bgu.spl.net.impl.stomp.Frame.ErrorFrame;
import bgu.spl.net.impl.stomp.Frame.ReceiptFrame;
import bgu.spl.net.impl.stomp.Frame.SendFrame;
import bgu.spl.net.impl.stomp.Frame.StompFrameAbstract;
import bgu.spl.net.impl.stomp.Frame.SubscribeFrame;
import bgu.spl.net.impl.stomp.Frame.UnsubscribeFrame;
import bgu.spl.net.srv.ConnectionsImpl;

public class StompProtocol implements StompMessagingProtocol<StompFrameAbstract> {
    
    private boolean shouldTerminate = false;
    private ConnectionsImpl<StompFrameAbstract> connections;
    private int connectionId;
    

    @Override
    public void start(int connectionId, ConnectionsImpl<StompFrameAbstract> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(StompFrameAbstract msg) {
        String command = msg.getCommand();
        switch (command) {
            case "CONNECT":
                connect((ConnectFrame) msg);
            case "SEND":
                send((SendFrame) msg);
            case "SUBSCRIBE":
                subscribe((SubscribeFrame) msg);
            case "UNSUBSCRIBE":
                unsubscribe((UnsubscribeFrame) msg);
            case "DISCONNECT":
                disconnect((DisconnectFrame) msg);
            default:
                connections.send(connectionId, new ErrorFrame("unknown command:" + command, connectionId, null, msg)); 
                
        }
    }

    private void connect(ConnectFrame frame) {
        String login = frame.getHeaders().get("login");
        String passcode = frame.getHeaders().get("passcode");
        if (login == null || passcode == null) {
            connections.send(connectionId, new ErrorFrame("missing login or passcode", connectionId, null, frame));
        }
        String s = connections.checkUser(login);
        if (s==null){
             connections.addUser(login, passcode);
        }
        else {
            if (passcode != s) {
                connections.send(connectionId, new ErrorFrame("wrong password", connectionId, null, frame));
        }   
            else{
                ;
            }

            }
        }
        connections.send(connectionId, new ConnectedFrame());
    }

    private void send(SendFrame frame) {
        String dest = frame.getHeaders().get("destination");
        if (dest == null) {
            ErrorFrame error = new ErrorFrame("missing destination", connectionId, null, frame);
            connections.send(connectionId, error);
            connections.disconnect(connectionId);
        }
        connections.send(dest, frame);
    //need to handle the errors here like the instructions
    }

    private void subscribe(SubscribeFrame frame) { //IMPLEMENT
        
    }

    private void unsubscribe(UnsubscribeFrame frame) { //IMPLEMENT

    }

    private void disconnect(DisconnectFrame frame) {
        this.shouldTerminate = true;
        String receiptId = frame.getHeaders().get("receipt");
        connections.send(connectionId, new ReceiptFrame(Integer.parseInt(receiptId)));
        connections.disconnect(connectionId);
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

}
