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
                break;
            case "SEND":
                send((SendFrame) msg);
                break;
            case "SUBSCRIBE":
                subscribe((SubscribeFrame) msg);
                break;
            case "UNSUBSCRIBE":
                unsubscribe((UnsubscribeFrame) msg);
                break;
            case "DISCONNECT":
                disconnect((DisconnectFrame) msg);
                break;
            default:
                connections.send(connectionId, new ErrorFrame("unknown command:" + command, connectionId, null, msg));
                break; 
                
        }
    }

    private void connect(ConnectFrame frame) {
        String login = frame.getHeaders().get("login");
        String passcode = frame.getHeaders().get("passcode");
        if (login == null || passcode == null) {
            connections.send(connectionId, new ErrorFrame("Missing login or passcode", connectionId, null, frame));
            connections.disconnect(connectionId);
            return;
        }
        String password = connections.checkUser(login);
        if (password == null) {
            connections.addUser(login, passcode); //new user
            connections.addActiveUser(connectionId, login); //pairs the user with handler
            connections.send(connectionId, new ConnectedFrame());
        }

        else if (!passcode.equals(password)) { //wrong password
            connections.send(connectionId, new ErrorFrame("Wrong password", connectionId, null, frame));
            connections.disconnect(connectionId); 
            return;
        }   

        else { //checks if the user already logged in         
            if (connections.connectedUser(login) == null) { //checkes if the user is availaible
                connections.addActiveUser(connectionId, login); //pairs the user with handler
                connections.send(connectionId, new ConnectedFrame());
            }
            else { 
                connections.send(connectionId, new ErrorFrame("User already logged in", connectionId, null, frame));
                connections.disconnect(connectionId);
                return;
            }
        }
    }

    private void send(SendFrame frame) {

        String dest = frame.getHeaders().get("destination");
        // if (dest != null && dest.startsWith("/")) {
        //     dest = dest.substring(1);
        // }

        if (dest == null) {
            ErrorFrame error = new ErrorFrame("Missing destination", connectionId, null, frame);
            connections.send(connectionId, error);
            connections.disconnect(connectionId);
            return;
        }
        ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> channelSubscribers = connections.getChannelSub();
        // if (!channelSubscribers.containsKey(dest) || !channelSubscribers.get(dest).containsKey(connectionId)) {
        //     connections.send(connectionId, new ErrorFrame("user not subscribed to the channel", connectionId, null, frame));
        //     //DISCONNECT?
        //     return;
        // }

        ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> userSubscriptions = connections.getSub();
        try {
            ConcurrentHashMap<Integer, Boolean> destSub = channelSubscribers.get(dest);
            if (!destSub.isEmpty()) {
                for (Integer id : destSub.keySet()) {
                    ConcurrentHashMap<String, Integer> userSub = userSubscriptions.get(id);
                   if (!userSub.isEmpty()) {
                    int subId = userSub.get(dest);
                    MessageFrame broadcast = new MessageFrame(subId, dest, frame.getBody()); //sending a message frame with each uniqe subId
                        if (!connections.send(id, broadcast)) {
                            connections.send(id, new ErrorFrame("couldn't send message - connection terminated", connectionId, null, frame));
                            connections.disconnect(id);
                        }
                   }
                }
            }
        } catch (NullPointerException e) {}

    }

    private void subscribe(SubscribeFrame frame) { 
        String dest = frame.getHeaders().get("destination");
        String subIdStr = frame.getHeaders().get("id");
        
        // if (dest != null && dest.startsWith("/")) {
        //     dest = dest.substring(1);
        // }

        if (dest == null || subIdStr == null) {
            connections.send(connectionId, new ErrorFrame("Missing destination or id", connectionId, null, frame));
            connections.disconnect(connectionId);
            return;
        }

        int subId;
        try {
            subId = Integer.valueOf(subIdStr);
        } catch (NumberFormatException e) {
            connections.send(connectionId, new ErrorFrame("Invalid subscription id", connectionId, null, frame));
            connections.disconnect(connectionId);
            return;
        }

        if (!connections.subscribe(connectionId, dest, subId)) {
            connections.send(connectionId, new ErrorFrame("User already subscribed to this channel", connectionId, null, frame));
            return;
        }
        
    }

    private void unsubscribe(UnsubscribeFrame frame) { 
        String subIdStr = frame.getHeaders().get("id");
        if (subIdStr == null) {
            connections.send(connectionId, new ErrorFrame("Missing id", connectionId, null, frame));
            connections.disconnect(connectionId);
            return;
        }

        int subId;
        try {
            subId = Integer.valueOf(subIdStr);
        } catch (NumberFormatException e) {
            connections.send(connectionId, new ErrorFrame("Invalid subscription id", connectionId, null, frame));
            connections.disconnect(connectionId);
            return;
        }

        if (!connections.unsubscribe(connectionId, subId)) {
            connections.send(connectionId, new ErrorFrame("Not subscribe to channel", connectionId, null, frame));
        }
    }

    private void disconnect(DisconnectFrame frame) {
        String receiptIdStr = frame.getHeaders().get("receipt");
        if (receiptIdStr == null) {
            connections.send(connectionId, new ErrorFrame("Missing receipt id", connectionId, null, frame));
            connections.disconnect(connectionId);
            return;
        }

        int receiptId;
        try {
            receiptId = Integer.valueOf(receiptIdStr);
            connections.send(connectionId, new ReceiptFrame(receiptId));
            connections.disconnect(connectionId);
            this.shouldTerminate = true;
        } catch (NumberFormatException e) {
            connections.send(connectionId, new ErrorFrame("Invalid receipt id", connectionId, null, frame));
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }


}
