package bgu.spl.net.srv;


public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void sendAllSub(String channel, T msg);

    void disconnect(int connectionId);
}
