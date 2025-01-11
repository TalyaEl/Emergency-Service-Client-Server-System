package bgu.spl.net.srv;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import bgu.spl.net.impl.stomp.Frame.ErrorFrame;
import bgu.spl.net.impl.stomp.Frame.MessageFrame;
import bgu.spl.net.srv.NonBlockingConnectionHandler;

public class ConnectionsImpl<T> implements Connections<T> {
    // private static final AtomicInteger counter = new AtomicInteger(1);
    // private final int msgId;
    private ConcurrentHashMap<Integer, NonBlockingConnectionHandler<T>> handlers;
    private ConcurrentHashMap<Integer, Set<String>> clientSubscriptions;
    private ConcurrentHashMap<String, Set<Integer>> channelSubscribers;

    public ConnectionsImpl() {
        // this.msgId = counter.getAndIncrement();
        this.handlers = new ConcurrentHashMap<>();
        this.channelSubscribers = new ConcurrentHashMap<>();
        this.clientSubscriptions = new ConcurrentHashMap<>();
    }

    @Override
    public boolean send(int connectionId, T msg) {
        if (handlers.containsKey(connectionId)) {
            NonBlockingConnectionHandler<T> handler = handlers.get(connectionId);
            handler.send(msg);
            return true;
        }
        return false;
    }

    @Override
    public void send(String channel, T msg) {
        Set<Integer> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            for (Integer sub : subscribers) {
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
    public void disconnect(int connectionId) {
        if (handlers.containsKey(connectionId)) {
            NonBlockingConnectionHandler<T> handler = handlers.get(connectionId);
            handler.close();
            Set<String> set = clientSubscriptions.get(connectionId);
            for (String channel : set) { //for each channel, remove this client from it's subscribers
                channelSubscribers.get(channel).remove(connectionId);
            }
            handlers.remove(connectionId);
            clientSubscriptions.remove(connectionId);
        }
    }
}
