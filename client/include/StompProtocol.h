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
string username; //current username
bool isLoggedIn; // true if user is logged in
int nextSubsctiptionId; // counter to generate next subscriprion id
std::map<string,int > channelSubs; // hashmap for <channel, subscriptionId>
std::vector<string> myChannels;
std::vector<Event> events; // data structure to store events


// hashmap for reciptId 
public:
StompProtocol(int connectionId);
void proccesKeyboardInput(const std::vector<string>& args);
StompFrame login(string hostPort, string username, string password);
StompFrame join(string channel);
StompFrame exit(string channel);
StompFrame report(string json_path);
StompFrame logout();
StompFrame summary(string channel,string user, string txtName);
bool isSubscribed(string channel);

void StompProtocol::proccesRecievedFrame(const string& args);
bool StompProtocol::connected();
bool StompProtocol::message();
};
