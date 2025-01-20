package bgu.spl.net.srv;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections<T> {
    private final AtomicInteger clientId = new AtomicInteger(0);
    private ConcurrentHashMap<String, String> loginInfo;
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> activeUsers;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> channelSubscribers;

    public ConnectionsImpl() {
        this.loginInfo = new ConcurrentHashMap<>();
        this.activeUsers = new ConcurrentHashMap<>();
        this.channelSubscribers = new ConcurrentHashMap<>();
    }

   public synchronized void addConnection(ConnectionHandler<T> handler) {
        if (handler != null) {
            int connectionId = clientId.incrementAndGet();
            activeUsers.put(connectionId, handler);
        }
        else {
            throw new IllegalArgumentException("Handler cannot be null");
        }
    }


    @Override
    public boolean send(int connectionId, T msg) {
        if (activeUsers.containsKey(connectionId)) {
            ConnectionHandler<T> handler = activeUsers.get(connectionId);
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
    public void send(String channel, T msg) {
        ConcurrentHashMap<Integer, Boolean> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            for (Integer sub : subscribers.keySet()) {
                send(sub, msg);
            }
        }
    }

    @Override
    public synchronized void disconnect(int connectionId) {
        if (activeUsers.containsKey(connectionId)) {
            ConnectionHandler<T> handler = activeUsers.get(connectionId);
            try {
                handler.close(); //
            } catch (IOException e) {}
        }
        for (String channel : channelSubscribers.keySet()) {
            ConcurrentHashMap<Integer, Boolean> subscribers = channelSubscribers.get(channel);
            if (subscribers != null) {
                subscribers.remove(connectionId);
            }
        }
    }

    public synchronized void subscribe(int connectionId, String channel) {
        if (activeUsers.containsKey(connectionId)) {
            channelSubscribers.putIfAbsent(channel, new ConcurrentHashMap<>());
            channelSubscribers.get(channel).putIfAbsent(connectionId, true);
        }
    }

    public synchronized void unsubscribe(int connectionId, String channel) {
        ConcurrentHashMap<Integer, Boolean> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            subscribers.remove(connectionId);
        }
    }
}
