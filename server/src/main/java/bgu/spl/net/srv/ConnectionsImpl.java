package bgu.spl.net.srv;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ConcurrentHashMap;

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
                return true; 
            } catch (Exception e) {
                return false;
            }
        }
        else {
            return false;
        }
        
    }

    @Override
    public void sendAllSub(String channel, T msg) {
        return; //chose not to use this function at all
    }

    @Override
    public synchronized void disconnect(int connectionId) {
        if (activeUsers.containsKey(connectionId)) {
            ConnectionHandler<T> handler = activeUsers.get(connectionId).getValue();
            activeUsers.remove(connectionId);
            try {
                handler.close(); 
            } catch (IOException e) {}
        }
        userSubscriptions.remove(connectionId);
        channelSubscribers.values().forEach(channel -> channel.remove(connectionId));
    }

    public synchronized void subscribe(int connectionId, String channel, int subId) { //helper
        if (activeUsers.containsKey(connectionId)) { //checking if the user is active
            channelSubscribers.putIfAbsent(channel, new ConcurrentHashMap<>()); //if the channel doesn't exist, create it
            userSubscriptions.putIfAbsent(connectionId, new ConcurrentHashMap<>()); //if the user has no subscriptions, create it
            channelSubscribers.get(channel).putIfAbsent(connectionId, true);
            userSubscriptions.get(connectionId).putIfAbsent(channel, subId);


        }
    }

    public synchronized boolean unsubscribe(int connectionId, int subId) { //helper
        ConcurrentHashMap<String, Integer> subscriptions = userSubscriptions.get(connectionId);
        if (subscriptions == null) {
            return false;
        }
        String channel = null;
        for (String s : subscriptions.keySet()) {
            if (subscriptions.get(s) == subId) {
                channel = s;
                break;
            }
        }

        if (channel != null) {
            userSubscriptions.get(connectionId).remove(channel);
            ConcurrentHashMap<Integer, Boolean> subscribers = channelSubscribers.get(channel);
            if (subscribers != null) {
                subscribers.remove(connectionId);
            }
            return true;
        }
       return false;
    }

    public synchronized String checkUser(String user) { //helper
        if (loginInfo.containsKey(user))
          return loginInfo.get(user);
        return null;
    }

    public void addUser(String username, String password) { //helper
        loginInfo.put(username, password);
    }

    public synchronized String connectedUser(int connectionId) { //helper
        SimpleEntry<String, ConnectionHandler<T>> entry = activeUsers.get(connectionId);
        if (entry == null) {
            return null;
        }
        return entry.getKey();
    }

    public synchronized void addActiveUser(int connectionId, String user) { //helper
        SimpleEntry<String, ConnectionHandler<T>> entry = this.activeUsers.get(connectionId);
        if (entry != null) {
            SimpleEntry<String, ConnectionHandler<T>> updatedEntry = new SimpleEntry<>(user, entry.getValue());
            activeUsers.put(connectionId, updatedEntry);
        }
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> getChannelSub() { //helper
        return this.channelSubscribers;
    }

    public ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> getSub() { //helper
        return this.userSubscriptions;
    }


}
