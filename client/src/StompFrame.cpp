#include "../include/StompFrame.h"
#include <sstream>
using std::istringstream;

//constructor
StompFrame::StompFrame(string cmd, map<string, string> hdrs, string frameBody) :
    command(cmd), headers(hdrs), body(frameBody) {}


StompFrame StompFrame::parse(const string& stringFrame) {
    istringstream stream(stringFrame);
    string line;
    string command;
    map<string, string> headers;
    string body;

    //get the command line into command
    getline(stream, command);

    //get the headers from the string into the map
    while (getline(stream, line) && !line.empty()) {
        size_t colonIndex = line.find(":");
        if (colonIndex != string::npos) { //if colon is found
            headers[line.substr(0, colonIndex)] = line.substr(colonIndex + 1);
        }
    }

    string bodyLine;
    while (getline(stream, bodyLine)) { //trim new line from body
        body += bodyLine + "\n";
    }

    return StompFrame(command, headers, body);
}

 string StompFrame::serialize() const {
    
 }