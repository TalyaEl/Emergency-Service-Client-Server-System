#include "StompClient.h"
#include "ConnectionHandler.h"
#include "StompProtocol.h"
#include <iostream>
#include <sstream>

using std::make_unique;
using std::thread;

StompClient::StompClient() : connection(make_unique<ConnectionHandler>()), protocol(make_unique<StompProtocol>()) {}

StompClient::~StompClient() {
	stop();
}

void StompClient::connect (const string& host, short port) {
	try { 
		//initialize connection
		connection = make_unique<ConnectionHandler>(host, port);

		if (!connection -> connect()) { //if the handler couldn't connect to the server
			throw std::runtime_error("failed to connect to server");
		}

		socket_thread = thread()
	}
}


void StompClient::stop() {
    is_running = false;  //let the threads know to stop running
    
    if (socket_thread.joinable()) { //check if the socket thread is active
        socket_thread.join();  //if so, wait for it to finish the work
    }
    
    if (connection) { //checks that it's not null pointer
        connection->close();  //close network connection if exist
    }
}


int main(int argc, char *argv[]) {
	// TODO: implement the STOMP client
	return 0;
}