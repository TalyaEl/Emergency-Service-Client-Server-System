#include "../include/StompProtocol.h"
#include "../include/ConnectionHandler.h"
#include "../include/StompFrame.h"
#include <iostream>
#include <string>
#include <vector>
#include <set>
#include "/workspaces/Assignment3/client/src/ConnectionHandler.cpp"
using namespace std;
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
    report,
    logout,
    summary
};
keyCommand keyStringToCommand(const std::string& command) {
    if (command == "login") return login;
    if (command == "join") return join;
    if (command == "exit") return exitChannel;
    if (command == "report") return report;
    if (command == "logout") return logout;
    if (command == "summary") return summary;
}
StompProtocol::StompProtocol(int Id): 
handler(),connectionId(Id),username(),isLoggedIn(false),
nextSubsctiptionId(1),channelSubs(),myChannels(),events()
{}
void StompProtocol::proccesKeyboardInput(const std::vector<string>& args){
        string frameType= args[0];
        switch (keyStringToCommand(frameType)) {
            case keyCommand::login:
                if(args.size()==4){
                    login(args[1],args[2],args[3]);
                }
                else{
                    cout << "login command needs 3 args: {host:port} {username} {password}";
                }
                break;
            case keyCommand::join:
                if(args.size()==2){
                    join(args[1]);
                }
                else{
                    cout << "join command needs 1 args: {channel_name}";
                }
                break;
            case keyCommand::exitChannel:
                if(args.size()==2){
                    exit(args[1]);
                }
                else{
                    cout << "exit command needs 1 args: {channel_name}";
                }
                break;
            case keyCommand::report:
                if(args.size()==2){
                    report(args[1]);
                }
                else{
                    cout << "report command needs 1 args: {file}";
                }
                break;
            case keyCommand::logout:
                logout();
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
        StompFrame frame(nullptr, {"",""}, "");
        return frame;
    }
    else{
        username=user;
        this->handler= new ConnectionHandler(host, portNum);
        ans=handler->connect();
        if(ans==true){
            //sent connect frame
            map<string, string> hdrs;
            hdrs.insert({"accept-version","1.2"});
            hdrs.insert({"host","stomp.cs.bgu.ac.il"});
            hdrs.insert({"login",username});
            hdrs.insert({"passcode", password});
            StompFrame frame("CONNECT", hdrs, "\n");//create connection frame//check if needed a recipt frame;    
            return frame;
        }
    }
}
StompFrame StompProtocol::join(string channel){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first";
        StompFrame frame(nullptr, {"",""}, "");
        return frame;
    }

    if(isSubscribed(channel)){     //check correct input data
        cout << "cannot subscribe to a channel you are already subscribed to";
        StompFrame frame(nullptr, {"",""}, "");
        return frame;
    }
    map<string, string> hdrs;
    hdrs.insert({"destination",channel});
    hdrs.insert({"id",to_string(nextSubsctiptionId)});
    StompFrame frame("SUBSCRIBE", hdrs, "\n");//create subscription frame//check if needed a recipt frame; 
    return frame;   
    bool ans= handler->sendFrameAscii(frame.serialize(),'\0'); // send using handler.sendFrameAscii
    if(ans){//if was sent:
        myChannels.push_back(channel);//add to mychannles
        channelSubs.insert({channel,nextSubsctiptionId});//add to channlesubs
        nextSubsctiptionId++;//raise sub id ++
        cout << "Joined channel " + channel;//syso "Joined channel <channelname>"
        return true;
    }
    return false;
    //return if was sent
    
    
    
    
}
StompFrame StompProtocol::exit(string channel){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first";
        return false;
    }
    if(!isSubscribed(channel)){//check correct input data
        cout << "you are not subscribed to the channel" + channel;
        return false;
    } 
    int subId = channelSubs.find(channel)->second;
    map<string, string> hdrs;
    hdrs.insert({"id",to_string(subId)});
    StompFrame frame("UNSUBSCRIBE", hdrs, "\n");//create unsubscription frame//check if needed a recipt frame;    
    bool ans= handler->sendFrameAscii(frame.serialize(),'\0'); // send using handler.sendFrameAscii
    if(ans){
        myChannels.erase(std::remove(myChannels.begin(), myChannels.end(), channel), myChannels.end());    //remove to mychannles
        channelSubs.erase(channel);//remove to channlesubs
        cout << "Exited channel <channelname>";
        return true;
    }
    return false;
}
StompFrame StompProtocol::report(string json_path){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first";
        return false;
    }
    bool ans=true;
    names_and_events eventsTxt= parseEventsFile(json_path); //creating a name and events type
    string channel = eventsTxt.channel_name;// retreving the channel name
    if(!isSubscribed(channel)) {// checking if subscribed to channel
        cout << "you are not subscribed to channel " + channel;
        return false;
    }
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
        StompFrame frame("SEND", hdrs, body);
        if(!handler->sendFrameAscii(frame.serialize(), '\0')) {
            ans = false;
        }
    }
    if(ans==true){
        cout << "reported";
        return true;
    }

     



    //check correct input data
    //check if i am subscribed to channel if not syso "not subscribed to chanel"
    // create message frame
    // send using  handler.sendFrameAscii()
    // if sent correctly syso "Reported"
}
StompFrame StompProtocol::logout(){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first";
        return false;
    }
    else{
    StompFrame frame("DISCONNECT", {"recipt:",id}, "\n");//create unsubscription frame//check if needed a recipt frame;    
    handler->sendFrameAscii(frame.serialize(),'\0');//send disconnect frame
    isLoggedIn=false;//change isLoggedIn to false
    handler->close();
    handler->~ConnectionHandler();//call destructor
    }    
}
StompFrame StompProtocol::summary(string channel, string user ,string txtName){
    if(isLoggedIn==false){ //if not logged in ask the user to log in first
        cout << "please login first";
        return false;
    }
    //check correct input data
    //check if subscribed to channel
    //create txt file
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
void StompProtocol::proccesRecievedFrame(const string& args){
        StompFrame frame = StompFrame::parse(args);
        string frameType = frame.getCommand();
        switch (serverStringToCommand(frameType)) {
            case serverCommand::CONNECTED:
                connected();
                break;
            case serverCommand::MESSAGE:
                if(args.size()==2){
                    message(args[1]);
                }
                else{
                    cout << "join command needs 1 args: {channel_name}";
                }
                break;
            case serverCommand::RECIPT:
                if(args.size()==2){
                    exit(args[1]);
                }
                else{
                    cout << "exit command needs 1 args: {channel_name}";
                }
                break;
            case serverCommand::ERROR:
                if(args.size()==2){
                    report(args[1]);
                }
                else{
                    cout << "report command needs 1 args: {file}";
                }
                break;
            default:
                cout << "invalid command";
                break;
        }
}

bool StompProtocol::connected(){
    isLoggedIn=true;
    cout << "Login succesful";
    return true; 
}

bool StompProtocol::message(){



}


