#pragma once
#include <string>
#include "../include/event.h"
#include <vector>
#include <atomic>
#include <map>
using namespace std;
class StompFrame;




class StompProtocol
{
private:
std::atomic<int> connectionId; // conection id
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
StompProtocol();
StompFrame processKeyboardInput(const std::vector<std::string>& args);
StompFrame login(string hostPort, string username, string password);
StompFrame join(string channel);
StompFrame exit(string channel);
vector<StompFrame> report(string json_path);
StompFrame logout();
void summary(string channel,string user, string txtName);
bool isSubscribed(string channel);

void processReceivedFrame(const StompFrame& args);
void connectedFrame(const StompFrame& frame);
void messageFrame(const StompFrame& frame);
void reciptFrame(const StompFrame& frame);
void errorFrame(const StompFrame& frame);

bool loggedIn() const;
string epoch_to_date(int timestamp);
StompProtocol(const StompProtocol&) = delete;
StompProtocol& operator=(const StompProtocol&) = delete;
};
