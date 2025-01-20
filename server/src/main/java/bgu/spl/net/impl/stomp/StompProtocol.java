package bgu.spl.net.impl.stomp;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.StompMessagingProtocol;
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
            return;
        }

        String password = connections.checkUser(login);
        if (password == null) {
             connections.addUser(login, passcode); //new user
        }

        else if (!passcode.equals(password)) { //wrong password
            connections.send(connectionId, new ErrorFrame("wrong password", connectionId, null, frame));
            connections.disconnect(connectionId);
        }   

        else { //checks if the user already logged in
            if (connections.connectedUser(connectionId) == null) { //checkes if the handler is availaible
                connections.addActiveUser(connectionId, login); //pairs the user with handler
                connections.send(connectionId, new ConnectedFrame());
            }
            else { 
                connections.send(connectionId, new ErrorFrame("user already logged in", connectionId, null, frame));
                connections.disconnect(connectionId);
            }
        }
    }

    private void send(SendFrame frame) {
        String dest = frame.getHeaders().get("destination");
        ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> subscribers = connections.getChannelSub();
        if (!subscribers.get(dest).containsKey(connectionId)) {
            connections.send(connectionId, new ErrorFrame("user not subscribed to the channel", connectionId, null, frame));
            //DISCONNECT?
        }

        connections.sendAllSub(dest, frame);


        if (dest == null) {
            ErrorFrame error = new ErrorFrame("missing destination", connectionId, null, frame);
            connections.send(connectionId, error);
            connections.disconnect(connectionId);
        }
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
