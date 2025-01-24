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
int nextReciptId; //recipt id per client
int logoutId;
std::map<string,int > channelSubs; // hashmap for <channel, subscriptionId>
std::vector<string> myChannels;
std::vector<Event> events; // data structure to store events


// hashmap for reciptId 
public:
StompProtocol(int connectionId);
StompFrame processKeyboardInput(const std::vector<string>& args);
StompFrame login(string hostPort, string username, string password);
StompFrame join(string channel);
StompFrame exit(string channel);
vector<StompFrame> report(string json_path);
StompFrame logout();
void summary(string channel,string user, string txtName);
bool isSubscribed(string channel);

void StompProtocol::processReceivedFrame(const StompFrame& args);
bool StompProtocol::connected();
bool StompProtocol::message();
};
