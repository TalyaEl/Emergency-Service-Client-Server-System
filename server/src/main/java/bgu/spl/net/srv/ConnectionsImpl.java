package bgu.spl.net.srv;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import bgu.spl.net.impl.stomp.Frame.ErrorFrame;
import bgu.spl.net.impl.stomp.Frame.MessageFrame;
import bgu.spl.net.srv.NonBlockingConnectionHandler;

public class ConnectionsImpl<T> implements Connections<T> {
    private static final AtomicInteger clientId = new AtomicInteger(0);
    // private final int msgId;
    private ConcurrentHashMap<Integer, NonBlockingConnectionHandler<T>> handlers;
    private ConcurrentHashMap<Integer, ConcurrentHashMap<String,Boolean>> clientSubscriptions;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> channelSubscribers;

    public ConnectionsImpl() {
        // this.msgId = counter.getAndIncrement();
        this.handlers = new ConcurrentHashMap<>();
        this.channelSubscribers = new ConcurrentHashMap<>();
        this.clientSubscriptions = new ConcurrentHashMap<>();
    }

   public synchronized void addConnection(NonBlockingConnectionHandler<T> handler) {
        if (handler != null) {
            int connectionId = clientId.incrementAndGet();
            handlers.put(connectionId, handler);
            clientSubscriptions.put(connectionId, new ConcurrentHashMap<>());
        }
        else {
            throw new IllegalArgumentException("Handler cannot be null");
        }
    }


    @Override
    public boolean send(int connectionId, T msg) {
        if (handlers.containsKey(connectionId)) {
            NonBlockingConnectionHandler<T> handler = handlers.get(connectionId);
            handler.send(msg);
            // disconnect(sub);
            return true;
        }
        return false;
    }

    @Override
    public void send(String channel, T msg) {
        ConcurrentHashMap<Integer, Boolean> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            for (Integer sub : subscribers.keySet()) {
                // NonBlockingConnectionHandler<T> handler = handlers.get(sub);
                // MessageFrame frame = new MessageFrame(sub, msgId, channel, msg.toString());
                if (!send(sub, msg)) {
                    System.out.println("couldn't send, connection terminated for" + sub);
                    // ErrorFrame error = new ErrorFrame("couldn't send", sub, null, frame);
                    disconnect(sub);
                }
            }
        }
    }

    @Override
    public synchronized void disconnect(int connectionId) {
        if (handlers.containsKey(connectionId)) {
            NonBlockingConnectionHandler<T> handler = handlers.get(connectionId);
            handler.close();
            ConcurrentHashMap<String,Boolean> temp = clientSubscriptions.get(connectionId);
            for (String channel : temp.keySet()) { //for each channel, remove this client from it's subscribers
                channelSubscribers.get(channel).remove(connectionId);
            }
            handlers.remove(connectionId);
            clientSubscriptions.remove(connectionId);
        }
    }

    public synchronized void subscribe(int connectionId, String channel) {
        clientSubscriptions.get(connectionId).put(channel, Boolean.TRUE);
        channelSubscribers.putIfAbsent(channel, new ConcurrentHashMap<>());
        channelSubscribers.get(channel).putIfAbsent(connectionId, true);
    }

    public synchronized void unsubscribe(int connectionId, String channel) {
        ConcurrentHashMap<String, Boolean> subscriptions = clientSubscriptions.get(connectionId);
        if (subscriptions != null) {
            subscriptions.remove(channel);
        }
        ConcurrentHashMap<Integer, Boolean> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            subscribers.remove(connectionId);
        }
    }
}
