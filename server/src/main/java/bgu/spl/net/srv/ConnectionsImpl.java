package bgu.spl.net.srv;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.impl.stomp.Frame.ErrorFrame;
import bgu.spl.net.srv.NonBlockingConnectionHandler;
public class ConnectionsImpl<T> implements Connections<T> {
    
    private ConcurrentHashMap<Integer, NonBlockingConnectionHandler<T>> handlers;
    private ConcurrentHashMap<String, Set<Integer>> channelSubscribers;

    public ConnectionsImpl() {
        this.handlers = new ConcurrentHashMap<>();
        this.channelSubscribers = new ConcurrentHashMap<>();
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
                    NonBlockingConnectionHandler<T> handler = handlers.get(sub);
                    if (!send(sub, msg)) {
                        ErrorFrame error = new ErrorFrame(channel, 0, channel, null)
                        handler.close();
                    }
                }
        }
    }
}
