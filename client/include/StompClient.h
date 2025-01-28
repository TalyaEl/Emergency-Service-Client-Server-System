#pragma once
#include <vector>
#include <string>
#include <thread>
#include <atomic>
#include <memory>
using std::unique_ptr;
using std::thread;
using std::atomic;
using std::vector;

class ConnectionHandler;
class StompProtocol;
class StompFrame;

class StompClient {
private:
    unique_ptr<ConnectionHandler> connection;
    unique_ptr<StompProtocol> protocol;
    thread socket_thread;
    atomic<bool> is_running;
    bool open;

    void read_from_socket();
    void handleLogin(const std::vector<std::string>& args);
public:
    StompClient();
    ~StompClient();

    void process_keyboard_input();
    void stop();
};