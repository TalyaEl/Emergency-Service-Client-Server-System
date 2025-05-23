#include "../include/StompProtocol.h"
#include "../include/event.h"
#include "../include/StompFrame.h"
#include <iostream>
#include <string>
#include <atomic>
#include <vector>
#include <set>
#include <fstream>
#include <algorithm>
#include <ctime>
using namespace std;
using std::cout;

enum keyCommand {
    login,
    join,
    exitChannel,
    logout,
    summary,
    failed,
};
keyCommand keyStringToCommand(const std::string& command) {
    if (command == "login") return login;
    if (command == "join") return join;
    if (command == "exit") return exitChannel;
    if (command == "logout") return logout;
    if (command == "summary") return summary;
    return failed;
}
StompProtocol::StompProtocol(): 
connectionId(connectionId++),username(),isLoggedIn(false),
nextSubsctiptionId(1),nextReciptId(1),logoutId(-1),channelSubs(),myChannels(),events()
{}
StompFrame StompProtocol::processKeyboardInput(const std::vector<std::string>& args){
        string frameType= args[0];
        switch (keyStringToCommand(frameType)) {
            case keyCommand::login:
                if(args.size()==4){
                    return login(args[1],args[2],args[3]);
                }
                else{
                    cout << "login command needs 3 args: {host:port} {username} {password}"<<"\n";
                    return StompFrame();
                }
                break;
            case keyCommand::join:
                if(args.size()==2){
                   return join(args[1]);
                }
                else{
                    cout << "join command needs 1 args: {channel_name}"<<"\n";
                    return StompFrame();
                }
                break;
            case keyCommand::exitChannel:
                if(args.size()==2){
                    return exit(args[1]);
                }
                else{
                    cout << "exit command needs 1 args: {channel_name}"<<"\n";
                    return StompFrame();
                }
                break;

            case keyCommand::logout:
                return logout();
                break;
            case keyCommand::summary:
                if(args.size()==4){
                    summary(args[1], args[2], args[3]);
                }
                else{
                    cout << "summary command needs 3 args: {channel_name} {user} {file}"<<"\n";
                    return StompFrame();
                }
                break;
            default:
                cout << "invalid command"<<"\n";
                return StompFrame();
        }
        return StompFrame();

}
StompFrame StompProtocol::login(string hostPort, string user, string password){
    size_t i= hostPort.find(":");
    string host= hostPort.substr(0,i);
    if(isLoggedIn==true){
        cout << "The client is already logged in, log out before trying again" <<"\n";
        return StompFrame();
    }
    else{//sent connect frame
        username=user;
        map<string, string> hdrs;
        hdrs.insert({"accept-version","1.2"});
        hdrs.insert({"host","stomp.cs.bgu.ac.il"});
        hdrs.insert({"login",username});
        hdrs.insert({"passcode", password});
        string header= "CONNECT";
        string body=" ";
        StompFrame frame(header, hdrs, body);//create connection frame//check if needed a recipt frame;    
        return frame;
        }
    }
StompFrame StompProtocol::join(string channel){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first" <<"\n";
        return StompFrame();
    }

    if(isSubscribed(channel)){     //check correct input data
        cout << "cannot subscribe to a channel you are already subscribed to" <<"\n";
        return StompFrame();
    }
    map<string, string> hdrs;
    hdrs.insert({"destination",channel});
    hdrs.insert({"id",to_string(nextSubsctiptionId)});
    StompFrame frame("SUBSCRIBE", hdrs, "\n");//create subscription frame//check if needed a recipt frame; 
    myChannels.push_back(channel);//add to mychannles
    channelSubs.insert({channel,nextSubsctiptionId});//add to channlesubs
    nextSubsctiptionId++;//raise sub id ++
    cout << "Joined channel " + channel <<"\n";//syso "Joined channel <channelname>"
    return frame;   
}
StompFrame StompProtocol::exit(string channel){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first"<<"\n";
        return StompFrame();
    }
    if(!isSubscribed(channel)){//check correct input data
        cout << "you are not subscribed to the channel " + channel<<"\n";
        return StompFrame();
    } 
    int subId = channelSubs.find(channel)->second;
    map<string, string> hdrs;
    hdrs.insert({"id",to_string(subId)});
    StompFrame frame("UNSUBSCRIBE", hdrs, "\n");//create unsubscription frame//check if needed a recipt frame;
    myChannels.erase(std::remove(myChannels.begin(), myChannels.end(), channel), myChannels.end());    //remove to mychannles
    channelSubs.erase(channel);//remove to channlesubs
    cout << "Exited channel " << channel << "\n";
    return frame;

}
vector<StompFrame> StompProtocol::report(string json_path){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first"<<"\n";
        return vector<StompFrame>();
    }
    names_and_events eventsTxt= parseEventsFile(json_path); //creating a name and events type
    string channel = eventsTxt.channel_name;// retreving the channel name
    if(!isSubscribed(channel)) {// checking if subscribed to channel
        cout << "you are not subscribed to channel " + channel<<"\n";
        return vector<StompFrame>();
    }
    vector<StompFrame> report= vector<StompFrame>();
    for(Event& event : eventsTxt.events) {
        event.setEventOwnerUser(username);// editing the sender of the event
        map<string, string> hdrs = {{"destination", channel}};// editing the destination of the event
        string generalInfo;
        const auto& generalInfoMap = event.get_general_information();
        for(auto it = generalInfoMap.begin(); it != generalInfoMap.end(); ++it) {
            generalInfo += "  " + it->first + ":" + it->second + "\n";
        }
        string body = "user:" + username + "\n" +
                        "city: " + event.get_city() + "\n" +
                        "event name: " + event.get_name() + "\n" + 
                        "date time: " + std::to_string(event.get_date_time()) + "\n" +
                        "general information:" + "\n" + generalInfo +
                        "description:" + "\n" + event.get_description();
        string command = "SEND";
        StompFrame frame(command, hdrs, body);
        report.push_back(frame);
    }
        cout << "reported"<<"\n";
        return report;

}
StompFrame StompProtocol::logout(){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first"<<"\n";
        return StompFrame();
    }
    else{
    string command = "DISCONNECT";
    string body= "";
    map<string, string> hdrs ;
    hdrs.insert({"receipt",to_string(nextReciptId)});
    StompFrame frame(command, hdrs ,body);//create unsubscription frame//check if needed a recipt frame;    
    logoutId=nextReciptId;
    nextReciptId++;
    return frame;
    }    
}
void StompProtocol::summary(string channel, string user ,string txtName){
    if(isLoggedIn == false) { //if not logged in ask the user to log in first
        cout << "please login first" << "\n";
        return;
    }
    if (!isSubscribed(channel)) {
        cout << "You are not subscribed to channel " + channel << "\n";
        return;
    }

    std::ofstream outFile(txtName);
    outFile << "Channel " << channel <<"\n";
    outFile << "Stats:" << "\n";
    int total = 0;
    int active = 0;
    int forces = 0;

    for (const Event& event : events) { 
        if (event.getEventOwnerUser() == user && event.get_channel_name()== channel) {
            total++;
            const auto& info = event.get_general_information();

            if (info.find(" active") != info.end()) {  // Use find instead of at
                if (info.find(" active")->second == "true")
                    active++;
            }
            if (info.find(" forces_arrival_at_scene") != info.end()) {
                if (info.find(" forces_arrival_at_scene")->second == "true")
                    forces++;
            }

        }
    }
    outFile << "Total: " << total << "\n"
    << "Active: " << active << "\n"
    << "Forced Arrival: " << forces << "\n";
    total=0;
    for (const Event& event : events) {
        if (event.getEventOwnerUser() == user && event.get_channel_name()== channel) {
            total++;
            outFile << "Report_" << total << ":" << "\n"
                 << "  city: " << event.get_city() << "\n"
                 << "  date time: " << epoch_to_date(event.get_date_time()) << "\n"
                 << "  event name: " << event.get_name() << "\n";
                 string s = event.get_description();
                 if (s.size() <= 27 ){
                    outFile << "  summary: " << event.get_description();
                 }
                 else{
                    outFile << "  summary: " << event.get_description().substr(0, 26) << "..." << "\n";
                 }
        }
    }
    outFile.close();
    cout << "Summary written to: " << txtName <<"\n";

}
bool StompProtocol::isSubscribed(string channel){
    for(string c: myChannels){
        if(c== channel)
            return true;
    }
    return false;
}

enum serverCommand {
    CONNECTED,
    MESSAGE,
    RECEIPT,
    ERROR,
    DEFAULT,
};
serverCommand serverStringToCommand(const std::string& command) {
    if (command == "CONNECTED") return CONNECTED;
    if (command == "MESSAGE") return MESSAGE;
    if (command == "RECEIPT") return RECEIPT;
    if (command == "ERROR") return ERROR;
    return DEFAULT;

    
}
void StompProtocol::processReceivedFrame(const StompFrame& frame){
        string frameType = frame.getCommand();
        switch (serverStringToCommand(frameType)) {
            case serverCommand::CONNECTED:
                connectedFrame(frame);
                break;
            case serverCommand::MESSAGE:
                    messageFrame(frame);
                break;
            case serverCommand::RECEIPT:
                    reciptFrame(frame);
                break;
            case serverCommand::ERROR:
                    errorFrame(frame);
                    break;
            default:
                break;
        
    }
}

void StompProtocol::connectedFrame(const StompFrame& frame){
    isLoggedIn=true;
    cout << "Login succesful" << "\n";
}

void StompProtocol::messageFrame(const StompFrame& frame){
    string channel = frame.getHeader("destination");
     if (!channel.empty()) {
        string report = "channel name:" + channel + "\n";
        report = report + frame.getBody();
        Event event(report);
        events.emplace_back(event);
        sort(events.begin(), events.end(), [](const Event& e1, const Event& e2) {
            if (e1.get_date_time() == e2.get_date_time()) {
                return e1.get_name() < e2.get_name();
            }
            return e1.get_date_time() < e2.get_date_time();
        });
    }
}

void StompProtocol::reciptFrame(const StompFrame& frame){
    string recId= frame.getHeader("receipt-id");
    if(recId != ""){
        int Id = std::stoi(recId);
        if(Id == logoutId){
            isLoggedIn=false;
            myChannels.clear();
            nextSubsctiptionId=1;
            nextReciptId=1;
            channelSubs.clear();
            events.clear();
            cout << "logged out" << "\n";
        }
    }
}

void StompProtocol::errorFrame(const StompFrame& frame){
    string error = frame.getHeader("message");
    if(error != ""){
        string body= frame.getBody();
        string print= error + "\n" + body;
        cout << print;
    }       
    

}
bool StompProtocol::loggedIn() const {
    return isLoggedIn;
}
string StompProtocol::epoch_to_date(int timestamp) {
    std::time_t time = timestamp;
    std::tm* localTime = std::localtime(&time);
    char buffer[20];
    std::strftime(buffer, sizeof(buffer), "%d/%m/%Y %H:%M", localTime);
    return buffer;
}













