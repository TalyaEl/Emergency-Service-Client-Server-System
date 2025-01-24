#pragma once

#include <string>
#include <thread>
#include <atomic>
#include <memory>
using std::unique_ptr;
using std::thread;
using std::atomic;

class ConnectionHandler;
class StompProtocol;
class StompFrame;

class StompClient {
private:
    unique_ptr<ConnectionHandler> connection;
    unique_ptr<StompProtocol> protocol;
    thread socket_thread;
    atomic<bool> is_running{true};

    void read_from_socket();
    void handleLogin(const vector<string>& args);
public:
    StompClient();
    ~StompClient();

    void process_keyboard_input();
    void stop();
};