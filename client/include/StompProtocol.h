#pragma once
#include <string>
#include <vector>
#include <map>
#include "../include/ConnectionHandler.h"
#include "../include/event.h"
using namespace std;
// TODO: implement the STOMP protocol
class StompProtocol
{
private:
ConnectionHandler* handler;// conection handler
const int connectionId;// conection id
const string username; //current username
bool isLoggedIn; // true if user is logged in
int nextSubsctiptionId; // counter to generate next subscriprion id
std::map<string,int > channelSubs; // hashmap for <channel, subscriptionId>
std::vector<string> myChannels;
std::vector<Event> events; // data structure to store events


// hashmap for reciptId 
public:
StompProtocol(int connectionId, string username);
void frameNav(const std::vector<string>& args);
bool login(string host, string port, string username, string password);
bool join(string channel);
bool exit(string channel);
bool report(string message);
bool logout();
bool summary(string channel,string user, string txtName);
bool isSubscribed(string channel);
//bool processFrame(StompFrame frame);
};
