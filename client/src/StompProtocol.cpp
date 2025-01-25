#include "../include/StompProtocol.h"
#include "../include/ConnectionHandler.h"
#include "../include/StompFrame.h"
#include <iostream>
#include <string>
#include <atomic>
#include <vector>
#include <set>
#include <fstream>
#include <algorithm>
using namespace std;
using std::cout;
// const int connectionId;// conection id
// const string username; //current username
// bool isLoggedIn; // true if user is logged in
// int nextSubsctiptionId; // counter to generate next subscriprion id
//std::map<string,int > channelSubs; // hashmap for <channel, subscriptionId>
// std::vector<string> myChannels;
// std::vector<Event> events;
enum keyCommand {
    login,
    join,
    exitChannel,
//    report,
    logout,
    summary
};
keyCommand keyStringToCommand(const std::string& command) {
    if (command == "login") return login;
    if (command == "join") return join;
    if (command == "exit") return exitChannel;
//    if (command == "report") return report;
    if (command == "logout") return logout;
    if (command == "summary") return summary;
}
StompProtocol::StompProtocol(): 
handler(),connectionId(connectionId.fetch_add(1)),username(),isLoggedIn(false),
nextSubsctiptionId(1),nextReciptId(1),logoutId(-1),channelSubs(),myChannels(),events()
{}
StompFrame StompProtocol::processKeyboardInput(const std::vector<string>& args){
        string frameType= args[0];
        switch (keyStringToCommand(frameType)) {
            case keyCommand::login:
                if(args.size()==4){
                    return login(args[1],args[2],args[3]);
                }
                else{
                    cout << "login command needs 3 args: {host:port} {username} {password}";
                }
                break;
            case keyCommand::join:
                if(args.size()==2){
                   return join(args[1]);
                }
                else{
                    cout << "join command needs 1 args: {channel_name}";
                }
                break;
            case keyCommand::exitChannel:
                if(args.size()==2){
                    return exit(args[1]);
                }
                else{
                    cout << "exit command needs 1 args: {channel_name}";
                }
                break;
            // case keyCommand::report:
                // if(args.size()==2){
                //     return report(args[1]);
                // }
                // else{
                //     cout << "report command needs 1 args: {file}";
                // }
                // break;
            case keyCommand::logout:
                return logout();
                break;
            case keyCommand::summary:
                if(args.size()==3){
                    summary(args[1], args[2], args[3]);
                }
                else{
                    cout << "summary command needs 3 args: {channel_name} {user} {file}";
                }
                break;
            default:
                cout << "invalid command";
                break;
        }
}
StompFrame StompProtocol::login(string hostPort, string user, string password){
    size_t i= hostPort.find(":");
    string host= hostPort.substr(0,i);
    string port= hostPort.substr(i+1);
    short portNum= std::stoi(port);
    bool ans;
    if(isLoggedIn==true){
        cout << "The client is already logged in, log out before trying again";
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
        cout << "please login first";
        return StompFrame();
    }

    if(isSubscribed(channel)){     //check correct input data
        cout << "cannot subscribe to a channel you are already subscribed to";
        return StompFrame();
    }
    map<string, string> hdrs;
    hdrs.insert({"destination",channel});
    hdrs.insert({"id",to_string(nextSubsctiptionId)});
    StompFrame frame("SUBSCRIBE", hdrs, "\n");//create subscription frame//check if needed a recipt frame; 
    myChannels.push_back(channel);//add to mychannles
    channelSubs.insert({channel,nextSubsctiptionId});//add to channlesubs
    nextSubsctiptionId++;//raise sub id ++
    cout << "Joined channel " + channel;//syso "Joined channel <channelname>"
    return frame;   
}
StompFrame StompProtocol::exit(string channel){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first";
        return StompFrame();
    }
    if(!isSubscribed(channel)){//check correct input data
        cout << "you are not subscribed to the channel" + channel;
        return StompFrame();
    } 
    int subId = channelSubs.find(channel)->second;
    map<string, string> hdrs;
    hdrs.insert({"id",to_string(subId)});
    StompFrame frame("UNSUBSCRIBE", hdrs, "\n");//create unsubscription frame//check if needed a recipt frame;
    myChannels.erase(std::remove(myChannels.begin(), myChannels.end(), channel), myChannels.end());    //remove to mychannles
    channelSubs.erase(channel);//remove to channlesubs
    cout << "Exited channel <channelname>";
    return frame;

}
vector<StompFrame> StompProtocol::report(string json_path){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first";
        return vector<StompFrame>();
    }
    names_and_events eventsTxt= parseEventsFile(json_path); //creating a name and events type
    string channel = eventsTxt.channel_name;// retreving the channel name
    if(!isSubscribed(channel)) {// checking if subscribed to channel
        cout << "you are not subscribed to channel " + channel;
        return vector<StompFrame>();
    }
    vector<StompFrame> report= vector<StompFrame>();
    for(Event& event : eventsTxt.events) {
        event.setEventOwnerUser(username);// editing the sender of the event
        map<string, string> hdrs = {{"destination:", channel}};// editing the destination of the event
        string generalInfo;
        for(const auto& info : event.get_general_information()) {
            generalInfo += "  " + info.first + ": " + info.second + "\n";// stringing the general info of the event into one string
        }
        string body = "user: " + username + "\n" +
                        "city: " + event.get_city() + "\n" +
                        "event name: " + event.get_name() + "\n" + 
                        "date time: " + std::to_string(event.get_date_time()) + "\n" +
                        "general information:\n" + generalInfo +
                        "description:\n" + event.get_description();// building the body for the frame
        string command= "SEND";
        StompFrame frame(command, hdrs, body);
        report.push_back(frame);
    }
        cout << "reported";
        return report;
    //check correct input data
    //check if i am subscribed to channel if not syso "not subscribed to chanel"
    // create message frame
    // send using  handler.sendFrameAscii()
    // if sent correctly syso "Reported"
}
StompFrame StompProtocol::logout(){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first";
        return StompFrame();
    }
    else{
    string command = "DISCONNECT";
    string body= "";
    map<string, string> hdrs ;
    hdrs.insert({"recipt:",to_string(nextReciptId)});
    StompFrame frame(command, hdrs ,body);//create unsubscription frame//check if needed a recipt frame;    
    logoutId=nextReciptId;
    nextReciptId++;
    return frame;
    }    
}
void StompProtocol::summary(string channel, string user ,string txtName){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first";
        return;
    }
    if (!isSubscribed(channel)) {
        cout << "You are not subscribed to channel " + channel << endl;
        return;
    }
    std::ofstream outFile(txtName);
    outFile << "Channel " << channel << endl;
    outFile << "Stats:" << endl;
    int total = 0;
    int active = 0;
    int forces = 0;
    for (const Event& event : events) {
        if (event.getEventOwnerUser() == user && event.get_channel_name()== channel) {
            total++;
            if (event.get_general_information().at("active") == "true")
                active++;
            if (event.get_general_information().at("forces_arrival_at_scene") == "true") 
                forces++;
        }
    }
    outFile << "Total: " << total << "\n"
    << "Active: " << active << "\n"
    << "Forced Arrival: " << forces << "\n";
    total=0;
    for (const Event& event : events) {
        if (event.getEventOwnerUser() == user && event.get_channel_name()== channel) {
            total++;
            outFile << "Report " << total << ":\n"
                 << "  city: " << event.get_city() << "\n"
                 << "  date time: " << epoch_to_date(event.get_date_time()) << "\n"
                 << "  event name: " << event.get_name() << "\n"
                 << "  summary: " << event.get_description().substr(0, 27) << "...\n";

        }
    }
    outFile.close();
    cout << "Summary written to: " << txtName << endl;

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
    RECIPT,
    ERROR,
};
serverCommand serverStringToCommand(const std::string& command) {
    if (command == "CONNECTED") return CONNECTED;
    if (command == "MESSAGE") return MESSAGE;
    if (command == "RECIPT") return RECIPT;
    if (command == "ERROR") return ERROR;
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
            case serverCommand::RECIPT:
                    reciptFrame(frame);
                break;
            case serverCommand::ERROR:
                    errorFrame(frame);
            default:
                cout << "invalid command";
                break;
        
    }
}

void StompProtocol::connectedFrame(const StompFrame& frame){
    isLoggedIn=true;
    cout << "Login succesful";
}

void StompProtocol::messageFrame(const StompFrame& frame){
    string channel = frame.getHeader("destination");
    if(channel != ""){
        string body = frame.getBody();
        string eve= channel +"\n" + body;
        Event event(eve);
        events.push_back(event);
    }
}

void StompProtocol::reciptFrame(const StompFrame& frame){
    string recId= frame.getHeader("recipt-id");
    if(recId != ""){
        int Id = std::stoi(recId);
        if(Id == logoutId){
            isLoggedIn=false;
            handler->close();
            handler->~ConnectionHandler();
        }
    }
}

void StompProtocol::errorFrame(const StompFrame& frame){
    string error = frame.getHeader("message");
    if(error == ""){
        string body= frame.getBody();
        string print= error + "\n" + body;
        cout << print;
    }       
    

}

string epoch_to_date(int timestamp) {
    std::time_t time = timestamp;
    std::tm* localTime = std::localtime(&time);
    char buffer[20];
    std::strftime(buffer, sizeof(buffer), "%d/%m/%Y %H:%M", localTime);
    return buffer;
}













