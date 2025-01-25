#pragma once
#include <vector>
#include <string>
#include <map>
using std::string;
using std::map;

class StompFrame {
private:
   string command;
   map<string, string> headers;
   string body;

public:
    //constructors:
    StompFrame(); 
    StompFrame(string cmd, map<string, string> hdrs, string frameBody);

    static StompFrame parse(const string& stringFrame);

    string serialize() const; //similar to toString method

    //getters:
    string getCommand() const;
    map<string, string> getHeaders() const;
    string getBody() const;
    string getHeader(const string& key) const;
};