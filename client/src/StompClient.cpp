#include "StompClient.h"
#include "ConnectionHandler.h"
#include "StompProtocol.h"
#include "StompFrame.h"
#include <iostream>
#include <sstream>

using std::unique_ptr;
using std::thread;
using std::cerr;
using std::getline;
using std::exception;
using std::cin;
using std::endl;
using std::vector;
using std::string;

StompClient::StompClient() : 
    connection(nullptr),
    protocol(std::unique_ptr<StompProtocol>(new StompProtocol())),
    socket_thread(),
    is_running(true) {}

StompClient::~StompClient() {
	stop();
}

void StompClient::read_from_socket() {
    string inFrameStr;
    while (is_running) {
        if (!connection -> getFrameAscii(inFrameStr, '\0')) { //trying reading from the socket
            cerr << "Error reading from socket" << endl;
            is_running = false;
            break;
        }

    	//converting the inFrame string to a vector for the process
		vector<string> args;
		istringstream iss(inFrameStr);
        string word;
        while (iss >> word) {
            args.push_back(word);
        }

        try {
            if (args[0] == "RECEIPT") {
                connection -> ~ConnectionHandler(); 
            }
            else {
                StompFrame inFrame = StompFrame::parse(inFrameStr); //from string to frame
                cout << inFrame.getCommand() << "\n";// testtttttttttttttt
                protocol->processReceivedFrame(inFrame); //process incoming frame from server

            }
        }
        catch (const exception& e) {
            std::cerr << "Error processing received frame: " << e.what() << endl;
        }
    }
}

void StompClient::handleLogin(const vector<string>& args) {
    string host = args[1].substr(0, args[1].find(":"));
    string port = args[1].substr(args[1].find(":") + 1);
    string username = args[2];
    string password = args[3];

    connection = unique_ptr<ConnectionHandler>(new ConnectionHandler(host, std::stoi(port)));
    if (!connection->connect()) {
        throw std::runtime_error("Failed to connect to server");
    }

    socket_thread = thread(&StompClient::read_from_socket, this); //so the socket thread will be initialize only once
}

void StompClient::process_keyboard_input() {
    string input;
    while (is_running) {
        getline(cin, input);

		//converting the input string to a vector for the process
		vector<string> args;
		istringstream iss(input);
        string word;
        while (iss >> word) {
            args.push_back(word);
        }
        try {
			if (args[0] == "login") {
                StompFrame frame = protocol -> processKeyboardInput(args); //getting the connected frame
                if (frame.getCommand() != "") {
                    cout << frame.getCommand() << "\n";
                    handleLogin(args);
                    connection->sendFrameAscii(frame.serialize(), '\0');
                }            	
			}
			else if (args[0] == "report") { //to send a series of send frames - one for each event reported
				vector<StompFrame> eventsReported = protocol -> report(args[1]);
				if (!eventsReported.empty()) {
					for (StompFrame frame : eventsReported) {
						connection->sendFrameAscii(frame.serialize(), '\0');
					}
				}

			}
			else if (args[0] == "summary") {
				protocol -> processKeyboardInput(args);
			}
			//checks that it's not null pointer
			else{ //other cases rather than login and report and summary
				StompFrame frame = protocol -> processKeyboardInput(args);
                if (frame.getCommand() != "") {
                	connection->sendFrameAscii(frame.serialize(), '\0');
                }
			}
        }
        catch (const exception& e) {
            cerr << "Error processing command: " << e.what() << endl;
        }
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
	if (argc != 1) {
        std::cerr << "enter program name" << std::endl;
        return 1;
    }
    
    StompClient client;
    client.process_keyboard_input();	
	return 0;
}