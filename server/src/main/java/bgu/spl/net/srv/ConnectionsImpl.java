package bgu.spl.net.srv;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.NonBlockingConnectionHandler;
public class ConnectionsImpl<T> implements Connections {
    
    private ConcurrentHashMap<Integer, NonBlockingConnectionHandler> handlers;

    public ConnectionsImpl() {
        this.handlers = new ConcurrentHashMap<>();
    }

    @Override
    public boolean send(int connectionId, T msg) {

    }

}
