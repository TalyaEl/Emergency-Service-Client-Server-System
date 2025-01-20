package bgu.spl.net.srv;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.impl.stomp.Frame.MessageFrame;
import bgu.spl.net.impl.stomp.Frame.StompFrameAbstract;

public class ConnectionsImpl<T> implements Connections<T> {
    private ConcurrentHashMap<String, String> loginInfo;
    private ConcurrentHashMap<Integer, SimpleEntry<String, ConnectionHandler<T>>> activeUsers;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> channelSubscribers; //channel with list of connections id
    private ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> userSubscriptions; //connectionId with pair (channel, subId)

    public ConnectionsImpl() {
        this.loginInfo = new ConcurrentHashMap<>();
        this.activeUsers = new ConcurrentHashMap<>();
        this.channelSubscribers = new ConcurrentHashMap<>();
        this.userSubscriptions = new ConcurrentHashMap<>();
    }

   public synchronized void addConnection(int connectionId, ConnectionHandler<T> handler) { //helper
        if (handler != null) {
            activeUsers.put(connectionId, new SimpleEntry<>(null, handler)); 
        }
        else {
            throw new IllegalArgumentException("Handler cannot be null");
        }
    }

    @Override
    public boolean send(int connectionId, T msg) {
        if (activeUsers.containsKey(connectionId)) {
            ConnectionHandler<T> handler = activeUsers.get(connectionId).getValue();
            try {
                handler.send(msg);
            } catch (IllegalStateException e) {
                disconnect(connectionId);
                return false;
            }
        }
        return true; //maybe adding error frame here?
    }

    @Override
    public void sendAllSub(String channel, T msg) {
        //getting the body
        String frameString = msg.toString();
        String[] lines = frameString.split("\n");
        String body = parseBody(lines);
        //for each connection id, crating message frame and send to the subscriber
        for (Integer id : channelSubscribers.get(channel).keySet()) {
            int subId = userSubscriptions.get(id).get(channel);
            MessageFrame broadcast = new MessageFrame(subId, channel, body);
            send((int)id, broadcast);
        }
    }

    @Override
    public synchronized void disconnect(int connectionId) {
        if (activeUsers.containsKey(connectionId)) {
            ConnectionHandler<T> handler = activeUsers.get(connectionId).getValue();
            try {
                handler.close(); //MAKE SURE
            } catch (IOException e) {}
        }
        activeUsers.remove(connectionId);
        for (String channel : channelSubscribers.keySet()) {
            ConcurrentHashMap<Integer, Boolean> subscribers = channelSubscribers.get(channel);
            if (subscribers != null) {
                subscribers.remove(connectionId);
            }
        }
    }

    public synchronized void subscribe(int connectionId, String channel, int subId) { //helper
        if (activeUsers.containsKey(connectionId)) { //checking if the user is active
            channelSubscribers.putIfAbsent(channel, new ConcurrentHashMap<>()); //if the channel doesn't exist, create it
            channelSubscribers.get(channel).putIfAbsent(connectionId, true);
            userSubscriptions.putIfAbsent(connectionId, new ConcurrentHashMap<>()); //if the user has no subscriptions, create it
            userSubscriptions.get(connectionId).putIfAbsent(channel, subId);
        }
    }

    public synchronized void unsubscribe(int connectionId, String channel) { //helper
        ConcurrentHashMap<Integer, Boolean> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            subscribers.remove(connectionId);
        }
    }

    public synchronized String checkUser(String user) { //helper
        if (loginInfo.contains(user))
          return loginInfo.get(user);
        return null;
    }

    public void addUser(String username, String password) { //helper
        loginInfo.put(username, password);
    }

    public String connectedUser(int connectionId) { //helper
        return activeUsers.get(connectionId).getKey();
    }

    public synchronized void addActiveUser(int connectionId, String user) { //helper
        SimpleEntry<String, ConnectionHandler<T>> entry = this.activeUsers.get(connectionId);
        SimpleEntry<String, ConnectionHandler<T>> updatedEntry = new SimpleEntry<>(user, entry.getValue());
        activeUsers.put(connectionId, updatedEntry);
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> getChannelSub() { //helper
        return this.channelSubscribers;
    }

    public ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> getSub() { //helper
        return this.userSubscriptions;
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

}
