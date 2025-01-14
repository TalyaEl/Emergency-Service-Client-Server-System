package bgu.spl.net.srv;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import bgu.spl.net.impl.stomp.Frame.ErrorFrame;
import bgu.spl.net.impl.stomp.Frame.MessageFrame;
import bgu.spl.net.srv.NonBlockingConnectionHandler;

public class ConnectionsImpl<T> implements Connections<T> {
    private final AtomicInteger clientId = new AtomicInteger(0);
    private ConcurrentHashMap<Integer, NonBlockingConnectionHandler<T>> handlers;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> channelSubscribers;

    public ConnectionsImpl() {
        this.handlers = new ConcurrentHashMap<>();
        this.channelSubscribers = new ConcurrentHashMap<>();
    }

   public synchronized void addConnection(NonBlockingConnectionHandler<T> handler) {
        if (handler != null) {
            int connectionId = clientId.incrementAndGet();
            handlers.put(connectionId, handler);
        }
        else {
            throw new IllegalArgumentException("Handler cannot be null");
        }
    }


    @Override
    public boolean send(int connectionId, T msg) {
        if (handlers.containsKey(connectionId)) {
            NonBlockingConnectionHandler<T> handler = handlers.get(connectionId);
            if (handler.isClosed()) {
                disconnect(connectionId);
                System.out.println("couldn't send, connection terminated for" + connectionId);
            }
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
        if (handlers.containsKey(connectionId)) {
            NonBlockingConnectionHandler<T> handler = handlers.get(connectionId);
            handler.close();
            for (String channel : channelSubscribers.keySet()) {
                ConcurrentHashMap<Integer, Boolean> subscribers = channelSubscribers.get(channel);
                if (subscribers != null) {
                    subscribers.remove(connectionId);
                }
            }
        }
    }

    public synchronized void subscribe(int connectionId, String channel) {
        if (handlers.containsKey(connectionId)) {
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
