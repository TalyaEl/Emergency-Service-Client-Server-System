#include "../include/StompFrame.h"
#include <sstream>
using std::istringstream;
using std::stringstream;

//constructor
StompFrame::StompFrame() : command(""), headers(), body("") {}
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
            string key = line.substr(0, colonIndex - 1);
            string value = line.substr(colonIndex + 1);
            value.erase(0, value.find_first_not_of(" "));
            headers[key] = value;
        }
    }
    //get the body
    string bodyLine;
    while (getline(stream, bodyLine)) { //trim new line from body
        if (bodyLine[0] == '\0') break;
        body += bodyLine;
    }

    return StompFrame(command, headers, body);
}

string StompFrame::serialize() const {
    stringstream ss;
    ss << command << '\n';

    for (const auto& header : headers) {
        ss << header.first << ":" << header.second << "\n";
    }

    ss << "\n"; //add empty line between headers and body
    ss << body;
    ss << "\0";

    return ss.str();
}

//getters
string StompFrame::getCommand() const {return command;}
map<string, string> StompFrame::getHeaders() const {return headers;}
string StompFrame::getBody() const {return body;}

string StompFrame::getHeader(const string& key) const {
    auto it = headers.find(key); //iterator to the key in map
     //if the iterator isn't at the end of the map, return the value of key. otherwise, return empty string
    return (it != headers.end()) ? it->second : "";
}